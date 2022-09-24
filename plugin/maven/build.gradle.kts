plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm")
  id("de.benediktritter.maven-plugin-development") version "0.4.0"
}

group = "${Settings.groupId}.plugin.maven"
version = Settings.version

mavenPlugin {
  mojos {
    create("touch") {
      implementation = "community.flock.wirespec.plugin.maven.GeneratorMojo"
      description = "Maven plugin for wire-spec"
      parameters {
        parameter("outputDir", "java.io.File") {
          defaultValue = "\${project.build.outputDirectory}/myMojoOutput"
          isRequired = false
        }
      }
    }
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(project(":compiler:core"))
  implementation("org.apache.maven:maven-plugin-api:3.6.3")
  implementation("org.apache.maven.plugin-tools:maven-plugin-annotations:3.6.0")
  implementation("org.apache.maven:maven-project:2.2.1")
}
