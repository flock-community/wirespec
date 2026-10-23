package community.flock.wirespec.plugin.gradle

import io.kotest.core.spec.style.FunSpec
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Files

class CommandLineOptionsTest :
    FunSpec({

        test("several Wirespec tasks can be named in one invocation") {
            val projectDir = Files.createTempDirectory("wirespec-plugin").toFile()
            projectDir.resolve("settings.gradle.kts").writeText("")
            projectDir.resolve("build.gradle.kts").writeText(
                """
            import community.flock.wirespec.plugin.gradle.CompileWirespecTask
            import community.flock.wirespec.plugin.gradle.ConvertWirespecTask

            plugins {
                id("community.flock.wirespec.plugin.gradle")
            }

            tasks.register<CompileWirespecTask>("compileWirespec")
            tasks.register<ConvertWirespecTask>("convertWirespec")
                """.trimIndent(),
            )

            // Gradle reads a task's command-line options when another task name follows it.
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("--dry-run", "compileWirespec", "convertWirespec", "compileWirespec")
                .build()
        }
    })
