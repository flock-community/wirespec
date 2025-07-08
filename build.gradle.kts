plugins {
    id("root.publication")
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

group = libs.versions.group.id.get()
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
}

subprojects {
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

