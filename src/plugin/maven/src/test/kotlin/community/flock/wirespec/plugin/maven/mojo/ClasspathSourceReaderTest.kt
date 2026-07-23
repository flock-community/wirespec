package community.flock.wirespec.plugin.maven.mojo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class ClasspathSourceReaderTest :
    FunSpec({
        test("reads wirespec files from a jar without directory entries") {
            val jar = Files.createTempFile("wirespec-classpath", ".jar")
            try {
                JarOutputStream(Files.newOutputStream(jar)).use { output ->
                    mapOf(
                        "com/example/api/config/First.ws" to "endpoint First GET /first -> { 200 -> String }",
                        "com/example/api/config/nested/Second.ws" to "endpoint Second GET /second -> { 200 -> String }",
                        "com/example/api/other/Ignored.ws" to "endpoint Ignored GET /ignored -> { 200 -> String }",
                    ).forEach { (name, content) ->
                        output.putNextEntry(JarEntry(name))
                        output.write(content.toByteArray())
                        output.closeEntry()
                    }
                }

                listOf("com/example/api/config", "com.example.api.config").forEach { folder ->
                    readWirespecSourcesFromClasspathFolder(folder, listOf(jar.toFile()))
                        .map { it.name.value }
                        .sorted()
                        .shouldContainExactly("First", "Second")
                }
            } finally {
                Files.deleteIfExists(jar)
            }
        }
    })
