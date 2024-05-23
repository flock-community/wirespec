import Libraries.ARROW_CORE
import Libraries.KOTEST_ASSERTIONS
import Libraries.KOTEST_ASSERTIONS_ARROW
import Libraries.KOTLIN_STDLIB
import Versions.KOTLIN_COMPILER

plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("io.kotest.multiplatform")
}

group = "${Settings.GROUP_ID}.compiler"
version = Settings.version

repositories {
    mavenCentral()
}

kotlin {
    macosX64()
    macosArm64()
    linuxX64()
    js(IR) {
        nodejs()
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
        commonMain {
            dependencies {
                api(KOTLIN_STDLIB)
                api(ARROW_CORE)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation("community.flock.kotlinx.openapi.bindings:kotlin-openapi-bindings:0.0.24")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit"))
                implementation(KOTEST_ASSERTIONS)
                implementation(KOTEST_ASSERTIONS_ARROW)
            }
        }
    }
}
