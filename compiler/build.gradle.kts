import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests

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
    val buildLinux = buildAll || "WIRE_SPEC_BUILD_LINUX".fromEnv()
    val buildWindows = buildAll || "WIRE_SPEC_BUILD_WINDOWS".fromEnv()
    val buildMacOS = buildAll || "WIRE_SPEC_BUILD_MAC".fromEnv() || (!buildLinux && !buildWindows)

    if (buildMacOS) macosX64 { build() }
    if (buildLinux) linuxX64 { build() }
    if (buildWindows) mingwX64 { build() }
    js(IR) {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {}
        val desktopMain by creating {
            dependsOn(commonMain)
        }
        if (buildMacOS) {
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
        val jsMain by getting {
            dependsOn(commonMain)
        }
    }
}

fun String.fromEnv() = let(System::getenv).toBoolean()

fun KotlinNativeTargetWithHostTests.build() = binaries {
    executable {
        entryPoint = "community.flock.wirespec.main"
    }
}
