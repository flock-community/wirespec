package community.flock.wirespec.plugin.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class ClasspathWirespecTaskTest :
    FunSpec({

        fun project(buildScript: String): File = Files.createTempDirectory("wirespec-plugin").toFile().apply {
            resolve("settings.gradle.kts").writeText("")
            resolve("build.gradle.kts").writeText(
                """
                import community.flock.wirespec.plugin.Format
                import community.flock.wirespec.plugin.Language
                import community.flock.wirespec.plugin.gradle.CompileWirespecTask
                import community.flock.wirespec.plugin.gradle.ConvertWirespecTask
                import community.flock.wirespec.plugin.gradle.ClasspathWirespecTask

                plugins {
                    id("community.flock.wirespec.plugin.gradle")
                }

                val specs = configurations.create("specs")

                dependencies {
                    specs(files("specs.jar"))
                }

                """.trimIndent() + buildScript.trimIndent(),
            )
        }

        fun File.writeJar(files: Map<String, String>) = JarOutputStream(resolve("specs.jar").outputStream()).use { jar ->
            files.forEach { (name, content) ->
                jar.putNextEntry(JarEntry(name))
                jar.write(content.toByteArray())
                jar.closeEntry()
            }
        }

        fun File.run(task: String, configurationCache: Boolean = true) = GradleRunner.create()
            .withProjectDir(this)
            .withPluginClasspath()
            .withArguments(listOfNotNull(task, "--configuration-cache".takeIf { configurationCache }))

        fun File.fileNames(directory: String) = resolve(directory).walk().filter { it.isFile }.map { it.name }.toList()

        test("the Wirespec files beneath a folder on the classpath are the input of a compile task") {
            val projectDir = project(
                """
                val classpathWirespec = tasks.register<ClasspathWirespecTask>("classpathWirespec") {
                    classpathInput = "com.example.api"
                    classpath.from(specs)
                    output = layout.buildDirectory.dir("extracted")
                }

                tasks.register<CompileWirespecTask>("wirespec") {
                    input = classpathWirespec.flatMap { it.output }
                    output = layout.buildDirectory.dir("out")
                    language = Language.Kotlin
                    shared = false
                }
                """,
            )
            val specs = mapOf(
                "com/example/api/Pet.ws" to "type Pet { name: String }",
                "com/example/api/nested/Owner.ws" to "type Owner { name: String }",
                "com/example/other/Ignored.ws" to "type Ignored { name: String }",
            )
            projectDir.writeJar(specs)

            projectDir.run("wirespec").build().task(":classpathWirespec")?.outcome shouldBe TaskOutcome.SUCCESS
            projectDir.fileNames("build/extracted") shouldContainExactlyInAnyOrder listOf("Pet.ws", "Owner.ws")
            projectDir.fileNames("build/out") shouldContainAll listOf("Pet.kt", "Owner.kt")
            projectDir.fileNames("build/out") shouldNotContain "Ignored.kt"

            projectDir.run("wirespec").build().task(":classpathWirespec")?.outcome shouldBe TaskOutcome.UP_TO_DATE

            projectDir.writeJar(specs - "com/example/api/nested/Owner.ws" + ("com/example/api/Store.ws" to "type Store { name: String }"))
            projectDir.run("wirespec").build().task(":classpathWirespec")?.outcome shouldBe TaskOutcome.SUCCESS
            projectDir.fileNames("build/extracted") shouldContainExactlyInAnyOrder listOf("Pet.ws", "Store.ws")
            projectDir.fileNames("build/out") shouldContain "Store.kt"
        }

        test("a file on the classpath is the input of a convert task") {
            val projectDir = project(
                """
                val classpathOpenApi = tasks.register<ClasspathWirespecTask>("classpathOpenApi") {
                    classpathInput = "openapi/api.json"
                    classpath.from(specs)
                    output = layout.buildDirectory.dir("extracted")
                }

                tasks.register<ConvertWirespecTask>("convertWirespec") {
                    input = classpathOpenApi.flatMap { it.outputFile }
                    output = layout.buildDirectory.dir("out")
                    format = Format.OpenAPIV3
                    language = Language.Wirespec
                    preProcessor = { it.replace("OldName", "NewName") }
                }
                """,
            )
            projectDir.writeJar(
                mapOf(
                    "openapi/api.json" to
                        """
                        {
                          "openapi": "3.0.0",
                          "info": { "title": "Test", "version": "1.0.0" },
                          "paths": {},
                          "components": {
                            "schemas": {
                              "OldName": { "type": "object", "properties": { "id": { "type": "string" } } }
                            }
                          }
                        }
                        """.trimIndent(),
                ),
            )

            // ConvertWirespecTask needs a preProcessor, a lambda that the configuration cache cannot restore.
            projectDir.run("convertWirespec", configurationCache = false).build().task(":classpathOpenApi")?.outcome shouldBe TaskOutcome.SUCCESS

            val output = projectDir.resolve("build/out").walk().filter { it.isFile }.joinToString("\n") { it.readText() }
            output shouldContain "NewName"
            output shouldNotContain "OldName"
        }

        test("the task fails when the classpathInput is not on its classpath") {
            val projectDir = project(
                """
                tasks.register<ClasspathWirespecTask>("classpathWirespec") {
                    classpathInput = "com/example/missing"
                    classpath.from(specs)
                    output = layout.buildDirectory.dir("extracted")
                }
                """,
            )
            projectDir.writeJar(mapOf("com/example/api/Pet.ws" to "type Pet { name: String }"))

            projectDir.run("classpathWirespec").buildAndFail()
                .output shouldContain "Could not find com/example/missing on the classpath of the task."
        }

        test("the task fails when a folder holds two Wirespec files with the same name") {
            val projectDir = project(
                """
                tasks.register<ClasspathWirespecTask>("classpathWirespec") {
                    classpathInput = "com/example/api"
                    classpath.from(specs)
                    output = layout.buildDirectory.dir("extracted")
                }
                """,
            )
            projectDir.writeJar(
                mapOf(
                    "com/example/api/Pet.ws" to "type Pet { name: String }",
                    "com/example/api/v2/Pet.ws" to "type Pet { name: String, age: Integer }",
                ),
            )

            projectDir.run("classpathWirespec").buildAndFail()
                .output shouldContain "Found more than one Pet.ws at com/example/api on the classpath of the task."
        }
    })
