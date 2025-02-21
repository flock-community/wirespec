group = "${libs.versions.group.id.get()}.tools.playground"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
}

plugins.withId("maven-publish") {
    tasks.withType(AbstractPublishToMaven::class) {
        logger.info("Disabling $name task in project ${project.name}...")
        enabled = false
    }
}

task<Exec>("npmInstall") {
    commandLine("npm", "install")
}

task<Exec>("npmRunBuild") {
    dependsOn("npmInstall")
    commandLine("npm", "run", "build")
}

tasks.assemble {
    dependsOn("npmInstall")
}
