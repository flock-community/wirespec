plugins {
    java
    `maven-publish`
    id("org.jetbrains.kotlin.jvm")
    id("de.benediktritter.maven-plugin-development") version "0.4.1"
}

group = "${Settings.GROUP_ID}.plugin.maven"
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
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.10")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
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
