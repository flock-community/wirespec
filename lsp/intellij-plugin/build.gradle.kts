// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

plugins {
    java
    id("org.jetbrains.kotlin.jvm")
    id(Plugins.intellij.name) version Plugins.intellij.version
}

group = "${Settings.groupId}.lsp.intellij-plugin"
version = Settings.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":compiler:core"))
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2022.3")
    type.set("IC")
}

tasks.publishPlugin {
    channels.set(listOf("beta"))
    token.set(System.getenv("JETBRAINS_TOKEN"))
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
}

tasks {
    val createOpenApiSourceJar by registering(Jar::class) {
        // Java sources
        from(sourceSets.main.get().java) {
            include("**/community/flock/**/*.java")
        }

        destinationDirectory.set(layout.buildDirectory.dir("libs"))
        archiveClassifier.set("src")
    }

    buildPlugin {
        dependsOn(createOpenApiSourceJar)
        from(createOpenApiSourceJar) { into("scripts") }
    }
}
