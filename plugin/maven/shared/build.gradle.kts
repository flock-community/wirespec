plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
}

group = "${Settings.groupId}.plugin.maven.shared"
version = Settings.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":compiler:core"))
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
