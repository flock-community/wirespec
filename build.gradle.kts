plugins {
    id("root.publication")
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.spotless)
}

group = libs.versions.group.id.get()
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
}

val spotlessId = libs.plugins.spotless.get().pluginId
subprojects {
    apply(plugin = spotlessId)
    spotless {
        val exclude = listOf(
            "**/.github/**",
            "**/.gradle/**",
            "**/.idea/**",
            "**/.intellijPlatform/**",
            "**/.kotlin/**",
            "**/build/**",
            "**/vscode/**",
            "**/docs/**",
            "**/playground/**",
            "**/tmp/**",
            "**/generated/**",
            "**/resources/**",
            "**/node_modules/**",
            "**/*.lock",
            "**/*Emitter.kt",
        ).toTypedArray()

        format("misc") {
            target("**/*.md")
            targetExclude(*exclude)
            endWithNewline()
        }

        format("wirespec") {
            target("**/*.ws")
            targetExclude(*exclude)
            endWithNewline()
        }

        kotlin {
            target("**/*.kt", "**/*.kts")
            targetExclude(*exclude, "**/*Emitter.kt")
            ktlint().editorConfigOverride(
                mapOf("ktlint_code_style" to "intellij_idea"),
            )
            suppressLintsFor {
                step = "ktlint"
                shortCode = "standard:enum-entry-name-case"
            }
        }
    }

    afterEvaluate {
        val copyTestResourcesForJs by tasks.registering(Copy::class) {
            group = "nodejs"
            description = "Copy js specific test-resources for nodejs test task (located at src/*Test/resources)"

            logger.info("Copying test resources for ${project.path}")
            val projectFullName = project.path.replace(Project.PATH_SEPARATOR, "-")
            val buildDir = rootProject.layout.buildDirectory.get()
            from("$projectDir/src")
            include("*Test/resources/**/*")
            into("$buildDir/js/packages/${rootProject.name}$projectFullName-test/src")
        }

        if (project.tasks.findByName("jsNodeTest") != null) {
            project.tasks.named("jsNodeTest").configure {
                dependsOn(copyTestResourcesForJs)
            }
        }
    }
}
