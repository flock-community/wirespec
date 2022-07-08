import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("multiplatform") apply false
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

group = "${Settings.groupId}.lsp"
version = Settings.version

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
