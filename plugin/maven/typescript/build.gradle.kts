plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
    id("de.benediktritter.maven-plugin-development") version "0.4.0"
    id("maven-publish")
}

group = "${Settings.groupId}.plugin.maven.typescript"
version = Settings.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":compiler:core"))
    implementation(project(":plugin:maven:shared"))
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
    dependsOn(":plugin:maven:shared:publishToMavenLocal")
}
