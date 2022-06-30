import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version Versions.Languages.kotlin
    id("com.github.johnrengelman.shadow") version Versions.Plugins.shadow
}

group = "community.flock.wirespec.lsp"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementLSP()
    implementation(project(":lsp:jvm:core"))

    implementTesting()
}

tasks {

    getByName<Test>("test") {
        useJUnitPlatform()
    }

    getByName<ShadowJar>("shadowJar") {
        archiveBaseName.set("client")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "community.flock.wirespec.lsp.client.AppKt"))
        }
    }

    build {
        dependsOn(shadowJar)
    }

}
