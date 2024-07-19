// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

plugins {
    kotlin("multiplatform") apply false
    kotlin("jvm")
    alias(libs.plugins.intellij)
}

group = "${libs.versions.group.id.get()}.lsp.intellij-plugin"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":src:compiler:core"))
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2024.1")
    type.set("IC")
}

tasks.publishPlugin {
    channels.set(listOf("stable"))
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
