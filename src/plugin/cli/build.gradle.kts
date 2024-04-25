import Libraries.CLI_LIB
import Libraries.KOTEST_ASSERTIONS
import Libraries.KOTEST_ASSERTIONS_ARROW
import Libraries.KOTEST_ENGINE
import Versions.KOTLIN_COMPILER
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.goncalossilva.resources") version "0.4.0"
    id("io.kotest.multiplatform")
}

group = "${Settings.GROUP_ID}.plugin.cli"
version = Settings.version

repositories {
    mavenCentral()
}

kotlin {
    val projectName = name
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
        tasks {
            getByName<ShadowJar>("shadowJar") {
                archiveBaseName.set(projectName)
                mergeServiceFiles()
                manifest { attributes(mapOf("Main-Class" to "community.flock.wirespec.plugin.cli.MainKt")) }
            }
            build {
                dependsOn("shadowJar")
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
            dependencies {
                implementation("io.ktor:ktor-server-core:2.3.10")
                implementation("io.ktor:ktor-server-cio:2.3.10")
            }
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
