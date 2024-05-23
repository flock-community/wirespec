import Libraries.CLI_LIB
import Libraries.KOTEST_ASSERTIONS
import Libraries.KOTEST_ASSERTIONS_ARROW
import Libraries.KOTEST_ENGINE
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
        val desktopMain by creating {
            dependsOn(commonMain)
        }
        val macosX64Main by getting {
            dependsOn(desktopMain)
        }
        val macosArm64Main by getting {
            dependsOn(desktopMain)
        }
        val linuxX64Main by getting {
            dependsOn(desktopMain)
        }
        val jvmMain by getting {
            dependsOn(commonMain)
        }
        val jsMain by getting {
            dependencies {
                implementation(project(":src:compiler:lib"))
            }
            dependsOn(commonMain)
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
