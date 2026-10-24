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
                import community.flock.wirespec.compiler.core.ir.File
                import community.flock.wirespec.compiler.core.ir.IR
                import community.flock.wirespec.compiler.core.ir.RawElement
                import community.flock.wirespec.compiler.core.ir.extension.IrExtension
                import community.flock.wirespec.compiler.core.parse.ast.AST
                import community.flock.wirespec.integration.jvm.extension.JvmBuilderExtension
                import community.flock.wirespec.integration.kotlinxserialization.extension.KotlinxSerializationExtension
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

                class Marker : IrExtension {
                    override fun extend(ir: IR, ast: AST): IR = ir.map { element ->
                        if (element is File) element.copy(elements = listOf(RawElement("// marked")) + element.elements) else element
                    }
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

        test("the bundled extension classes ship with the plugin and apply to the languages they support") {
            val output = project("extensionClasses = listOf(KotlinxSerializationExtension::class.java, JvmBuilderExtension::class.java)").compile()

            output.getValue("Todo.kt") shouldContain "@kotlinx.serialization.Serializable"
            output.getValue("Todo.kt") shouldContain "fun toBuilder()"
            output.getValue("Todo.ts") shouldNotContain "kotlinx"
        }

        test("an extension class of your own applies to every language") {
            val output = project("extensionClasses = listOf(Marker::class.java)").compile()

            output.getValue("Todo.kt") shouldContain "// marked"
            output.getValue("Todo.ts") shouldContain "// marked"
        }
    })
