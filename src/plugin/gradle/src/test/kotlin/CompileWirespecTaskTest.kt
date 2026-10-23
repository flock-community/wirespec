package community.flock.wirespec.plugin.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.nio.file.Files

class CompileWirespecTaskTest :
    FunSpec({

        fun project(task: String): File = Files.createTempDirectory("wirespec-plugin").toFile().apply {
            resolve("settings.gradle.kts").writeText("")
            resolve("src/main/wirespec").mkdirs()
            resolve("src/main/wirespec/todo.ws").writeText(
                """
                type Todo {
                  name: String
                }
                """.trimIndent(),
            )
            resolve("build.gradle.kts").writeText(
                """
                import community.flock.wirespec.integration.jvm.extension.BuilderExtension
                import community.flock.wirespec.plugin.Extension
                import community.flock.wirespec.plugin.Language
                import community.flock.wirespec.plugin.gradle.CompileWirespecTask

                plugins {
                    id("community.flock.wirespec.plugin.gradle")
                }

                tasks.register<CompileWirespecTask>("compileWirespec") {
                    input = layout.projectDirectory.dir("src/main/wirespec")
                    output = layout.buildDirectory.dir("out")
                    languages = listOf(Language.Kotlin, Language.TypeScript)
                    shared = false
                    $task
                }
                """.trimIndent(),
            )
        }

        fun File.compile(): Map<String, String> {
            GradleRunner.create()
                .withProjectDir(this)
                .withPluginClasspath()
                .withArguments("compileWirespec")
                .build()
            return resolve("build/out").walk().filter { it.isFile }.associate { it.name to it.readText() }
        }

        test("bundled extensions apply to the languages they support") {
            val output = project("extensionNames = listOf(Extension.KotlinxSerialization, Extension.Builder)").compile()

            output.getValue("Todo.kt") shouldContain "@kotlinx.serialization.Serializable"
            output.getValue("Todo.kt") shouldContain "fun toBuilder()"
            output.getValue("Todo.ts") shouldNotContain "kotlinx"
        }

        test("the classes of the bundled extensions can be registered without adding them to the classpath") {
            val output = project("extensionClasses = listOf(BuilderExtension::class.java)").compile()

            output.getValue("Todo.kt") shouldContain "fun toBuilder()"
        }
    })
