import Versions.KOTLIN_COMPILER

plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.goncalossilva.resources") version "0.4.0"
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
                implementation("community.flock.kotlinx.openapi.bindings:kotlin-openapi-bindings:0.0.24")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:lib"))
                implementation(project(":src:plugin:cli"))
                implementation(project(":src:converter:openapi"))
                implementation(project(":src:tools:generator"))
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
            dependencies{
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit"))
                implementation("com.goncalossilva:resources:0.4.0")
            }
        }
    }
}
