plugins {
    java
    `maven-publish`
    id("org.jetbrains.kotlin.jvm")
    id("de.benediktritter.maven-plugin-development") version "0.4.1"
}

group = "${Settings.groupId}.plugin.maven"
version = Settings.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":src:compiler:core"))
    implementation(project(":src:openapi"))
    implementation("org.apache.maven:maven-plugin-api:3.9.1")
    implementation("org.apache.maven.plugin-tools:maven-plugin-annotations:3.8.1")
    implementation("org.apache.maven:maven-project:2.2.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.21")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.21")
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            artifactId = "wirespec-maven-plugin"
            from(components["java"])
        }
    }
}

tasks.publishToMavenLocal {
    dependsOn(":src:compiler:core:publishToMavenLocal")
    dependsOn(":src:openapi:publishToMavenLocal")
}

mavenPlugin {
    artifactId.set("wirespec-maven-plugin")
    description.set("Plugin to run wirespec compiler")
    goalPrefix.set("wirespec")
}

publishing {
    publications {
        create<MavenPublication>("wirespec-maven-plugin") {
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
