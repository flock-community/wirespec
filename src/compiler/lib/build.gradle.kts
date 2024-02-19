import Versions.KOTLIN_COMPILER

plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.github.johnrengelman.shadow") apply false
}

group = "${Settings.GROUP_ID}.compiler"
version = Settings.version

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        nodejs()
        generateTypeScriptDefinitions()
        binaries.library()
        compilations["main"].packageJson {
            customField("name", "@flock/wirespec-lib")
        }
    }
    jvm {
        withJava()
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
    }

    sourceSets.all {
        languageSettings.apply {
            languageVersion = KOTLIN_COMPILER
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:converter:openapi"))
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
        }
    }
}
