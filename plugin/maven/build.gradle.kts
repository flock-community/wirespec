plugins {
    id("java")
    id("de.benediktritter.maven-plugin-development") version "0.4.0"
    id("maven-publish")
    id("org.jetbrains.kotlin.jvm")
}

group = "${Settings.groupId}.plugin.maven"
version = Settings.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":compiler:core"))
    implementation("org.apache.maven:maven-plugin-api:3.6.3")
    implementation("org.apache.maven.plugin-tools:maven-plugin-annotations:3.6.0")
    implementation("org.apache.maven:maven-project:2.2.1")
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])

        }
    }
}

tasks.publishToMavenLocal {
    dependsOn(":compiler:core:publishToMavenLocal")
}
