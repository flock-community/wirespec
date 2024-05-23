import Libraries.KOTEST_ASSERTIONS
import Libraries.KOTEST_ASSERTIONS_ARROW

plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.goncalossilva.resources") version "0.4.0"
}

group = "${Settings.GROUP_ID}.converter"
version = Settings.version

repositories {
    mavenCentral()
    maven(uri("https://s01.oss.sonatype.org/service/local/repo_groups/public/content"))
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
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation("community.flock.kotlinx.openapi.bindings:kotlin-openapi-bindings:0.0.24")
            }
        }
        commonTest {
            dependencies {
                implementation("com.goncalossilva:resources:0.4.0")
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit"))
                implementation(KOTEST_ASSERTIONS)
                implementation(KOTEST_ASSERTIONS_ARROW)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("com.fasterxml.jackson.core:jackson-databind:2.9.8")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")

            }
        }
    }
}
