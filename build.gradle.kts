plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

group = libs.versions.group.id.get()
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
}

subprojects {
    // Explicit API mode: every declaration that ends up in a published artifact has to
    // state its visibility and its return type. KGP applies this to `main` compilations
    // and the common-metadata compilation only, so test and `codegen` compilations are
    // unaffected. The `withId` guards keep it off :src:bom and the :examples:* projects,
    // which apply no Kotlin plugin.
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> { explicitApi() }
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> { explicitApi() }
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

        project.tasks.findByName("jsNodeTest")?.let {
            project.tasks.named("jsNodeTest").configure {
                dependsOn(copyTestResourcesForJs)
            }
        }
    }
}
