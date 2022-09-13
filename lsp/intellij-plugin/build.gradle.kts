// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

plugins {
  id("java")
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

// Include the generated files in the source set
sourceSets {
  main {
    java {
      srcDirs("src/main/gen")
    }
  }
}

dependencies {
  implementation("com.github.ballerina-platform:lsp4intellij:0.95.0")

  implementation(project(":lsp:node:server"))
  implementation(project(":compiler:core"))
  implementation(project(":compiler:lib"))
  testImplementation("junit:junit:4.13.2")

}

java {
  sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
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

  patchPluginXml {
    version.set("${project.version}")
    sinceBuild.set("213")
    untilBuild.set("222.*")
  }

  test {
    // This path value is a machine-specific placeholder text.
    // Set idea.home.path to the absolute path to the intellij-community source
    // on your local machine. For real world projects, use variants described in:
    // https://docs.gradle.org/current/userguide/build_environment.html
    systemProperty("idea.home.path", "/Users/jhake/Documents/source/comm")
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
