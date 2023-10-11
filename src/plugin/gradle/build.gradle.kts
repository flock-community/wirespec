plugins {
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

group = "${Settings.GROUP_ID}.plugin.gradle"
version = Settings.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":src:compiler:core"))
}

gradlePlugin {
    val kotlin by plugins.creating {
        id = "${Settings.GROUP_ID}.plugin.gradle"
        implementationClass = "community.flock.wirespec.plugin.gradle.WirespecPlugin"
    }
}

tasks.publishToMavenLocal {
    dependsOn(":src:compiler:core:publishToMavenLocal")
}
