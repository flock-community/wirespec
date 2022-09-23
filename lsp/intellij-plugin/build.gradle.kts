// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.7.20-RC"
  id("org.jetbrains.intellij") version "1.9.0"
}

group = "${Settings.groupId}.lsp.intellij-plugin"
version = Settings.version

repositories {
  mavenCentral()
  maven{
    url = uri("https://jitpack.io")
  }
}

dependencies {
  implementation(project(":compiler:core"))
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
  version.set("2022.2.1")
  type.set("IU")
  plugins.set(listOf("JavaScript"))
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
