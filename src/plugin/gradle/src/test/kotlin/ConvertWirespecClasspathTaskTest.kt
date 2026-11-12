package community.flock.wirespec.plugin.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Files

class ConvertWirespecClasspathTaskTest :
    FunSpec({

        test("a file on the classpath is converted with the preProcessor applied") {
            val projectDir = Files.createTempDirectory("wirespec-plugin").toFile()
            projectDir.resolve("settings.gradle.kts").writeText("")
            projectDir.resolve("specs/openapi").mkdirs()
            projectDir.resolve("specs/openapi/api.json").writeText(
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
            )
            projectDir.resolve("build.gradle.kts").writeText(
                """
            import community.flock.wirespec.plugin.Format
            import community.flock.wirespec.plugin.Language
            import community.flock.wirespec.plugin.gradle.ConvertWirespecClasspathTask

            plugins {
                id("community.flock.wirespec.plugin.gradle")
            }

            tasks.register<ConvertWirespecClasspathTask>("convertWirespec") {
                classpathInput = "openapi/api.json"
                classpath.from("specs")
                output = layout.buildDirectory.dir("out")
                format = Format.OpenAPIV3
                language = Language.Wirespec
                preProcessor = { it.replace("OldName", "NewName") }
            }
                """.trimIndent(),
            )

            GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("convertWirespec")
                .build()

            val output = projectDir.resolve("build/out").walk().filter { it.isFile }.joinToString("\n") { it.readText() }
            output shouldContain "NewName"
            output shouldNotContain "OldName"
        }
    })
