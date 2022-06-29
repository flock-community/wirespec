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
    val buildAll = "WIRE_SPEC_BUILD_ALL".toBool()
    val buildLinux = buildAll || "WIRE_SPEC_BUILD_LINUX".toBool()
    val buildWindows = buildAll || "WIRE_SPEC_BUILD_WINDOWS".toBool()
    val buildMacOS = buildAll || "WIRE_SPEC_BUILD_MAC".toBool() || (!buildLinux && !buildWindows)

    if (buildAll || buildMacOS) macosX64 { build() }
    if (buildAll || buildLinux) linuxX64 { build() }
    if (buildAll || buildWindows) mingwX64 { build() }
}

fun String.toBool() = let(System::getenv).toBoolean()

fun KotlinNativeTargetWithHostTests.build() = binaries {
    executable {
        entryPoint = "community.flock.wirespec.main"
    }
}
