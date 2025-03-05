import com.github.gradle.node.npm.task.NpmTask

plugins {
    alias(libs.plugins.node.gradle.plugin)
}

group = "${libs.versions.group.id.get()}.tools"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

plugins.withId("maven-publish") {
    tasks.withType(AbstractPublishToMaven::class) {
        logger.info("Disabling $name task in project ${project.name}...")
        enabled = false
    }
}

task<NpmTask>("npmBuild") {
    args.set(listOf("run", "build"))
    dependsOn("npmInstall")
}

tasks.named("build") {
    dependsOn("npmBuild")
}

dependencies {
    project(":src:plugin:npm")
}
