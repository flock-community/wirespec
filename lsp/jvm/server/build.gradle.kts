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
        archiveBaseName.set("server")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "community.flock.wirespec.lsp.server.AppKt"))
        }
    }

    build {
        dependsOn(shadowJar)
    }

//    withType<Jar> {
//        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//
//        // Otherwise you'll get a "No main manifest attribute" error
//        manifest { attributes["Main-Class"] = "community.flock.wirespec.server.App" }
//
//        // Add all the dependencies otherwise a "NoClassDefFoundError" error
//        from(sourceSets.main.get().output)
//
//        dependsOn(configurations.runtimeClasspath)
//        from({
//            configurations.runtimeClasspath.get()
//                .filter { it.name.endsWith("jar") }
//                .map { zipTree(it) }
//        })
//    }

}
