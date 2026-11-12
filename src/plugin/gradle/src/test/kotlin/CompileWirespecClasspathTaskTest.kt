package community.flock.wirespec.plugin.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class CompileWirespecClasspathTaskTest :
    FunSpec({

        fun project(configuration: String): File = Files.createTempDirectory("wirespec-plugin").toFile().apply {
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
                import community.flock.wirespec.plugin.Language
                import community.flock.wirespec.plugin.gradle.CompileWirespecClasspathTask

                plugins {
                    id("community.flock.wirespec.plugin.gradle")
                }

                val specs = configurations.create("specs")

                dependencies {
                    specs(files("specs.jar"))
                }

                tasks.register<CompileWirespecClasspathTask>("wirespec") {
                    output = layout.buildDirectory.dir("out")
                    language = Language.Kotlin
                    shared = false
                    $configuration
                }
                """.trimIndent(),
            )
        }

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
            val projectDir = project(
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
            val projectDir = project(
                """
                classpathInput = "wirespec/todo.ws"
                classpath.from("src/main")
                """.trimIndent(),
            )

            projectDir.wirespec().build()
            projectDir.outputNames() shouldContain "Todo.kt"
        }

        test("a task fails when the classpathInput is not on its classpath") {
            project("""classpathInput = "wirespec/missing.ws"""")
                .wirespec()
                .buildAndFail()
                .output shouldContain "Could not find wirespec/missing.ws on the classpath of the task."
        }

        test("a task needs a classpathInput") {
            project("")
                .wirespec()
                .buildAndFail()
                .output shouldContain "property 'classpathInput' doesn't have a configured value"
        }
    })
