package community.flock.wirespec.plugin.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
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

class CompileWirespecTaskTest :
    FunSpec({

        fun project(buildScript: String): File = Files.createTempDirectory("wirespec-plugin").toFile().apply {
            resolve("settings.gradle.kts").writeText("")
            resolve("src/main/wirespec").mkdirs()
            resolve("src/main/wirespec/todo.ws").writeText(
                """
                type Todo {
                  name: String
                }
                """.trimIndent(),
            )
            resolve("build.gradle.kts").writeText(buildScript)
        }

        fun extendedProject(extensionClasses: String): File = project(
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

            tasks.register<CompileWirespecTask>("wirespecKotlin") {
                input = layout.projectDirectory.dir("src/main/wirespec")
                output = layout.buildDirectory.dir("out/kotlin")
                language = Language.Kotlin
                shared = false
                $extensionClasses
            }

            tasks.register<CompileWirespecTask>("wirespecTypeScript") {
                input = layout.projectDirectory.dir("src/main/wirespec")
                output = layout.buildDirectory.dir("out/typescript")
                language = Language.TypeScript
                shared = false
                $extensionClasses
            }

            class Marker : IrExtension {
                override fun extend(ir: IR, ast: AST): IR = ir.map { element ->
                    if (element is File) element.copy(elements = listOf(RawElement("// marked")) + element.elements) else element
                }
            }
            """.trimIndent(),
        )

        fun File.compile(): Map<String, String> {
            GradleRunner.create()
                .withProjectDir(this)
                .withPluginClasspath()
                .withArguments("wirespecKotlin", "wirespecTypeScript")
                .build()
            return resolve("build/out").walk().filter { it.isFile }.associate { it.name to it.readText() }
        }

        test("the bundled extension classes ship with the plugin and apply to the languages they support") {
            val output = extendedProject("extensionClasses = listOf(KotlinxSerializationExtension::class.java, JvmBuilderExtension::class.java)").compile()

            output.getValue("Todo.kt") shouldContain "@kotlinx.serialization.Serializable"
            output.getValue("Todo.kt") shouldContain "fun toBuilder()"
            output.getValue("Todo.ts") shouldNotContain "kotlinx"
        }

        test("an extension class of your own applies to every language") {
            val output = extendedProject("extensionClasses = listOf(Marker::class.java)").compile()

            output.getValue("Todo.kt") shouldContain "// marked"
            output.getValue("Todo.ts") shouldContain "// marked"
        }

        test("a task needs a language") {
            val projectDir = project(
                """
                import community.flock.wirespec.plugin.gradle.CompileWirespecTask

                plugins {
                    id("community.flock.wirespec.plugin.gradle")
                }

                tasks.register<CompileWirespecTask>("wirespec") {
                    input = layout.projectDirectory.dir("src/main/wirespec")
                    output = layout.buildDirectory.dir("out")
                }
                """.trimIndent(),
            )

            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("wirespec")
                .buildAndFail()
                .output shouldContain "property 'language' doesn't have a configured value"
        }

        fun classpathProject(configuration: String): File = project(
            """
            import community.flock.wirespec.plugin.Language
            import community.flock.wirespec.plugin.gradle.CompileWirespecTask

            plugins {
                id("community.flock.wirespec.plugin.gradle")
            }

            val specs = configurations.create("specs")

            dependencies {
                specs(files("specs.jar"))
            }

            tasks.register<CompileWirespecTask>("wirespec") {
                output = layout.buildDirectory.dir("out")
                language = Language.Kotlin
                shared = false
                $configuration
            }
            """.trimIndent(),
        )

        fun File.writeJar(files: Map<String, String>) = JarOutputStream(outputStream()).use { jar ->
            files.forEach { (name, content) ->
                jar.putNextEntry(JarEntry(name))
                jar.write(content.toByteArray())
                jar.closeEntry()
            }
        }

        fun File.wirespec() = GradleRunner.create()
            .withProjectDir(this)
            .withPluginClasspath()
            .withArguments("wirespec", "--configuration-cache")

        fun File.outputNames() = resolve("build/out").walk().filter { it.isFile }.map { it.name }.toList()

        test("the Wirespec files beneath a folder on the classpath are compiled") {
            val projectDir = classpathProject(
                """
                classpathInput = "com.example.api"
                classpath.from(specs)
                """.trimIndent(),
            )
            val specs = mapOf(
                "com/example/api/Pet.ws" to "type Pet { name: String }",
                "com/example/api/nested/Owner.ws" to "type Owner { name: String }",
                "com/example/other/Ignored.ws" to "type Ignored { name: String }",
            )
            projectDir.resolve("specs.jar").writeJar(specs)

            projectDir.wirespec().build().task(":wirespec")?.outcome shouldBe TaskOutcome.SUCCESS
            projectDir.outputNames() shouldContainAll listOf("Pet.kt", "Owner.kt")
            projectDir.outputNames() shouldNotContain "Ignored.kt"

            projectDir.wirespec().build().task(":wirespec")?.outcome shouldBe TaskOutcome.UP_TO_DATE

            projectDir.resolve("specs.jar").writeJar(specs + ("com/example/api/Store.ws" to "type Store { name: String }"))
            projectDir.wirespec().build().task(":wirespec")?.outcome shouldBe TaskOutcome.SUCCESS
            projectDir.outputNames() shouldContain "Store.kt"
        }

        test("one Wirespec file on the classpath is compiled") {
            val projectDir = classpathProject(
                """
                classpathInput = "wirespec/todo.ws"
                classpath.from("src/main")
                """.trimIndent(),
            )

            projectDir.wirespec().build()
            projectDir.outputNames() shouldContain "Todo.kt"
        }

        test("a task fails when the classpathInput is not on its classpath") {
            classpathProject("""classpathInput = "wirespec/missing.ws"""")
                .wirespec()
                .buildAndFail()
                .output shouldContain "Could not find wirespec/missing.ws on the classpath of the task."
        }

        test("a task needs either an input or a classpathInput") {
            classpathProject("")
                .wirespec()
                .buildAndFail()
                .output shouldContain "Set the input or the classpathInput."

            classpathProject(
                """
                input = layout.projectDirectory.dir("src/main/wirespec")
                classpathInput = "wirespec"
                """.trimIndent(),
            )
                .wirespec()
                .buildAndFail()
                .output shouldContain "Set the input or the classpathInput, not both."
        }
    })
