import Versions.KOTLIN_COMPILER

plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.github.johnrengelman.shadow") apply false
}

group = "${Settings.GROUP_ID}.plugin.npm"
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
            customField("name", "@flock/wirespec")
            customField("bin", mapOf("wirespec" to "wirespec-bin.js"))
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
                implementation(project(":src:compiler:lib"))
                implementation(project(":src:plugin:cli"))
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
        }
    }
}
