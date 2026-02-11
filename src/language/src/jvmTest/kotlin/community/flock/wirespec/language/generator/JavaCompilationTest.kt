package community.flock.wirespec.language.generator

import community.flock.wirespec.language.core.Package
import community.flock.wirespec.language.core.RawExpression
import community.flock.wirespec.language.core.Type
import community.flock.wirespec.language.core.file
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import kotlin.test.assertTrue

class JavaCompilationTest {

    private val wirespecSource = """
        package community.flock.wirespec.java;
        import java.util.*;
        public interface Wirespec {
            public interface Endpoint {}
            public interface Handler {}
            public interface Request<T> { public interface Headers {} }
            public interface Response<T> { public interface Headers {} public Integer getStatus(); }
            public interface Queries {}
            public interface Path {}
            public interface Serializer {
                <T> List<String> serializeParam(T value, java.lang.reflect.Type type);
                <T> Object serializeBody(T body, java.lang.reflect.Type type);
            }
            public interface Deserializer {
                <T> Optional<T> deserializeParam(List<String> value, java.lang.reflect.Type type);
                <T> T deserializeBody(Object body, java.lang.reflect.Type type);
            }
            public record RawRequest(String method, List<String> path, Map<String, List<String>> queries, Map<String, String> headers, Object body) {
                public Map<String, List<String>> queries() { return queries; }
            }
            public record RawResponse(int statusCode, Map<String, String> headers, Object body) {
                public int statusCode() { return statusCode; }
                public Object body() { return body; }
            }
            public static java.lang.reflect.Type getType(Class<?> type, Class<?> container) { return null; }
        }
    """.trimIndent()

    @Test
    fun testSimpleCompilation() {
        val todo = file("Todo") {
            `package`("com.example.model")
            struct("Todo") {
                field("id", string)
                field("done", boolean)
            }
        }

        val handler = file("TodoHandler") {
            `package`("com.example.handler")
            import("community.flock.wirespec.java", "Wirespec")
            import("com.example.model", "Todo")
            `interface`("TodoHandler") {
                extends(type("Wirespec.Handler"))
                asyncFunction("getTodo") {
                    returnType(type("Todo"))
                }
            }
        }

        assertCompiles(listOf(todo, handler))
    }

    @Test
    fun testComplexCompilation() {
        val todo = file("Todo") {
            `package`("com.example.model")
            struct("Todo") {
                field("name", string)
            }
        }

        val queries = file("Queries") {
            `package`("com.example.endpoint")
            import("community.flock.wirespec.java", "Wirespec")
            struct("Queries") {
                implements(type("Wirespec.Queries"))
                field("done", boolean.nullable())
            }
        }

        val request = file("Request") {
            `package`("com.example.endpoint")
            import("community.flock.wirespec.java", "Wirespec")
            struct("Request") {
                implements(type("Wirespec.Request", Type.Unit))
                field("queries", type("Queries"))
                constructo {
                    arg("done", boolean.nullable())
                    assign(
                        "this.queries",
                        construct(type("Queries")) {
                            arg("done", RawExpression("done"))
                        },
                    )
                }
            }
        }

        val response = file("Response") {
            `package`("com.example.endpoint")
            import("community.flock.wirespec.java", "Wirespec")
            `interface`("Response") {
                extends(type("Wirespec.Response", type("Object")))
                function("getStatus") {
                    returnType(integer)
                    returns(literal(0))
                }
            }
        }

        val response200 = file("Response200") {
            `package`("com.example.endpoint")
            import("com.example.model", "Todo")
            struct("Response200") {
                implements(type("Response"))
                field("body", list(type("Todo")))
                function("getStatus") {
                    returnType(integer)
                    returns(literal(200))
                }
            }
        }

        val endpoint = file("GetTodos") {
            `package`("com.example.endpoint")
            import("community.flock.wirespec.java", "Wirespec")
            import("com.example.model", "Todo")

            static("GetTodos") {
                function("fromRequest") {
                    returnType(type("Request"))
                    arg("serialization", type("Wirespec.Deserializer"))
                    arg("request", type("Wirespec.RawRequest"))
                    returns(
                        construct(type("Request")) {
                            arg(
                                "done",
                                functionCall("serialization.deserializeParam") {
                                    arg("value", RawExpression("request.queries().get(\"done\")"))
                                    arg(
                                        "type",
                                        functionCall("Wirespec.getType") {
                                            arg("type", RawExpression("Boolean.class"))
                                            arg("container", RawExpression("java.util.Optional.class"))
                                        },
                                    )
                                },
                            )
                        },
                    )
                }

                function("fromResponse") {
                    returnType(type("Response"))
                    arg("serialization", type("Wirespec.Deserializer"))
                    arg("response", type("Wirespec.RawResponse"))
                    switch(functionCall("response.statusCode")) {
                        case(literal(200)) {
                            returns(RawExpression("new Response200((java.util.List<Todo>) serialization.deserializeBody(response.body(), Wirespec.getType(Todo.class, java.util.List.class)))"))
                        }
                        default {
                            error(RawExpression("\"Unknown status\""))
                        }
                    }
                }
            }
        }

        assertCompiles(listOf(todo, queries, request, response, response200, endpoint))
    }

    private fun assertCompiles(files: List<community.flock.wirespec.language.core.File>) {
        val tempDir = Files.createTempDirectory("wirespec-compiler-test").toFile()
        try {
            // Write Wirespec base
            val wirespecDir = File(tempDir, "community/flock/wirespec/java")
            wirespecDir.mkdirs()
            File(wirespecDir, "Wirespec.java").writeText(wirespecSource)

            // Write generated files
            files.forEach { file ->
                val output = JavaGenerator.generate(file)
                val pkg = file.elements.filterIsInstance<Package>().firstOrNull()?.path ?: ""
                val pkgDir = File(tempDir, pkg.replace(".", "/"))
                pkgDir.mkdirs()
                File(pkgDir, "${file.name}.java").writeText(output)
            }

            val compiler = ToolProvider.getSystemJavaCompiler()
            val diagnostics = DiagnosticCollector<JavaFileObject>()
            val fileManager = compiler.getStandardFileManager(diagnostics, null, null)

            val filesToCompile = tempDir.walkTopDown().filter { it.extension == "java" }.toList()
            val compilationUnits = fileManager.getJavaFileObjectsFromFiles(filesToCompile)

            val task = compiler.getTask(null, fileManager, diagnostics, emptyList(), null, compilationUnits)
            val success = task.call()

            if (!success) {
                diagnostics.diagnostics.forEach {
                    System.err.println("[DEBUG_LOG] [${it.kind}] ${it.getMessage(null)}")
                    if (it.source != null) {
                        System.err.println("[DEBUG_LOG] [FILE] ${it.source}")
                        try {
                            val content = it.source.getCharContent(true).toString()
                            val lines = content.lines()
                            if (it.lineNumber > 0 && it.lineNumber <= lines.size) {
                                System.err.println("[DEBUG_LOG] [LINE ${it.lineNumber}] ${lines[it.lineNumber.toInt() - 1]}")
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    System.err.println("[DEBUG_LOG] [LINE] ${it.lineNumber}")
                }
            }

            assertTrue(success, "Compilation failed")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
