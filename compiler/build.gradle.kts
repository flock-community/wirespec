import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

plugins {
    kotlin("multiplatform") version Versions.Languages.kotlin
}

group = "community.flock.wirespec"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    val buildAll = "WIRE_SPEC_BUILD_ALL".fromEnv()
    val buildNode = buildAll || "WIRE_SPEC_BUILD_NODE".fromEnv()
    val buildMac = buildAll || "WIRE_SPEC_BUILD_MAC".fromEnv()
    val buildWindows = buildAll || "WIRE_SPEC_BUILD_WINDOWS".fromEnv()
    val buildLinux = buildAll || "WIRE_SPEC_BUILD_LINUX".fromEnv() || (!buildNode && !buildMac && !buildWindows)

    if (buildMac) macosX64 { build() }
    if (buildLinux) linuxX64 { build() }
    if (buildWindows) mingwX64 { build() }
    if (buildNode) js(IR) { build() }

    sourceSets {
        val commonMain by getting {}
        val desktopMain by creating {
            dependsOn(commonMain)
        }
        if (buildMac) {
            val macosX64Main by getting {
                dependsOn(desktopMain)
            }
        }
        if (buildLinux) {
            val linuxX64Main by getting {
                dependsOn(desktopMain)
            }
        }
        if (buildWindows) {
            val mingwX64Main by getting {
                dependsOn(desktopMain)
            }
        }
        if (buildNode) {
            val jsMain by getting {
                dependsOn(commonMain)
            }
        }
    }
}

fun String.fromEnv() = let(System::getenv).toBoolean()

fun KotlinNativeTargetWithHostTests.build() = binaries {
    executable {
        entryPoint = "community.flock.wirespec.main"
    }
}

fun KotlinJsTargetDsl.build() {
    nodejs()
    binaries.executable()
}
