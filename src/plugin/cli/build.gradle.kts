import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

plugins {
    kotlin("multiplatform")
    id("com.goncalossilva.resources") version "0.4.0"
    alias(libs.plugins.kotest)
}

group = "${libs.versions.group.id.get()}.plugin.cli"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

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
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }

    sourceSets.all {
        languageSettings.apply {
            languageVersion = libs.versions.kotlin.compiler.get()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":src:plugin:arguments"))
                implementation(project(":src:compiler:core"))
                implementation(project(":src:converter:openapi"))
                implementation(libs.clikt)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit"))
                implementation(libs.bundles.kotest)
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
