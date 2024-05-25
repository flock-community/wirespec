plugins {
    kotlin("multiplatform") apply false
    kotlin("jvm")
    `java-gradle-plugin`
}

group = "${libs.versions.group.id.get()}.plugin.gradle"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":src:compiler:core"))
    implementation(project(":src:plugin:arguments"))
}

java {
    withSourcesJar()
}

gradlePlugin {
    val kotlin by plugins.creating {
        id = "${libs.versions.group.id.get()}.plugin.gradle"
        implementationClass = "community.flock.wirespec.plugin.gradle.WirespecPlugin"
        displayName = "Wirespec gradle plugin"
        description = "Plugin for compiling Wirespec files"
    }
}


tasks.publishToMavenLocal {
    dependsOn(":src:compiler:core:publishToMavenLocal")
    dependsOn(":src:plugin:arguments:publishToMavenLocal")
}
