plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("org.jetbrains.kotlin.jvm")
}

group = "${Settings.groupId}.plugin.gradle"
version = Settings.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":compiler:core"))
}


gradlePlugin {
    val kotlin by plugins.creating {
        id = "${Settings.groupId}.plugin.gradle"
        implementationClass = "community.flock.wirespec.plugin.gradle.WirespecPlugin"
    }
}

tasks.publishToMavenLocal {
    dependsOn(":compiler:core:publishToMavenLocal")
}

publishing {
    publications {
        create<MavenPublication>("wirespec") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            credentials {
                username = System.getenv("JFROG_USERNAME")
                password = System.getenv("JFROG_TOKEN")
            }
            name = "flock-maven"
            url = uri("https://flock.jfrog.io/artifactory/flock-maven")
        }
    }
}
