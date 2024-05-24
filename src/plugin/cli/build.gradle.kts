import Libraries.CLI_LIB
import Libraries.KOTEST_ASSERTIONS
import Libraries.KOTEST_ASSERTIONS_ARROW
import Libraries.KOTEST_ENGINE
import Versions.JAVA
import Versions.KOTLIN_COMPILER
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.goncalossilva.resources") version "0.4.0"
    id("io.kotest.multiplatform")
}

group = "${Settings.GROUP_ID}.plugin.cli"
version = Settings.version

repositories {
    mavenCentral()
}

kotlin {
    macosX64 { build() }
    macosArm64 { build() }
    linuxX64 { build() }
    js(IR) { build() }
    jvm {
        withJava()
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(JAVA))
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
                implementation(project(":src:plugin:arguments"))
                implementation(project(":src:compiler:core"))
                implementation(project(":src:converter:openapi"))
                implementation(CLI_LIB)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit"))
                implementation(KOTEST_ENGINE)
                implementation(KOTEST_ASSERTIONS)
                implementation(KOTEST_ASSERTIONS_ARROW)
            }
        }
        val nativeMain by creating {}
        val macosX64Main by getting {}
        val macosArm64Main by getting {}
        val linuxX64Main by getting {}
        val jvmMain by getting {}
        val jsMain by getting {
            dependencies {
                implementation(project(":src:compiler:lib"))
            }
        }
    }
}

fun KotlinNativeTargetWithHostTests.build() {
    binaries {
        executable {
            entryPoint = "community.flock.wirespec.plugin.cli.main"
        }
    }
}

fun KotlinJsTargetDsl.build() {
    nodejs()
    binaries.executable()
}
