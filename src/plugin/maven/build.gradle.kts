plugins {
    kotlin("multiplatform") apply false
    kotlin("jvm")
    id("de.benediktritter.maven-plugin-development") version "0.4.3"
}

group = "${libs.versions.group.id.get()}.plugin.maven"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":src:compiler:core"))
    implementation(project(":src:converter:openapi"))
    implementation(project(":src:plugin:arguments"))
    implementation(libs.kotlin.reflect)
    implementation("org.apache.maven:maven-plugin-api:3.9.1")
    implementation("org.apache.maven.plugin-tools:maven-plugin-annotations:3.8.1")
    implementation("org.apache.maven:maven-project:2.2.1")
}

java {
    withSourcesJar()
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
    dependsOn(":src:converter:openapi:publishToMavenLocal")
    dependsOn(":src:plugin:arguments:publishToMavenLocal")
}

mavenPlugin {
    artifactId.set("wirespec-maven-plugin")
    description.set("Plugin to run wirespec compiler")
    goalPrefix.set("wirespec")
}
