import Libraries.CLI_LIB
import Libraries.KOTEST_ASSERTIONS
import Libraries.KOTEST_ASSERTIONS_ARROW
import Libraries.KOTEST_ENGINE
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.github.johnrengelman.shadow")
    id("com.goncalossilva.resources") version "0.4.0"
    id("io.kotest.multiplatform")
}

group = "${Settings.GROUP_ID}.compiler"
version = Settings.version

repositories {
    mavenCentral()
}

kotlin {
    val projectName = name
    val buildAll = "WIRESPEC_BUILD_ALL".fromEnv()
    val buildMacX86 = buildAll || "WIRESPEC_BUILD_MAC_X86".fromEnv()
    val buildMacArm = buildAll || "WIRESPEC_BUILD_MAC_ARM".fromEnv()
    val buildLinux = buildAll || "WIRESPEC_BUILD_LINUX".fromEnv()
    val buildNothing = !buildMacX86 && !buildMacArm && !buildLinux
    val buildJvm = buildAll || "WIRESPEC_BUILD_JVM".fromEnv() || buildNothing

    if (buildMacX86) macosX64 { build() }
    if (buildMacArm) macosArm64 { build() }
    if (buildLinux) linuxX64 { build() }
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
                manifest { attributes(mapOf("Main-Class" to "community.flock.wirespec.compiler.cli.MainKt")) }
            }
            build {
                dependsOn("shadowJar")
            }
        }
    }

    sourceSets.all {
        languageSettings.apply {
            languageVersion = "1.9"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:openapi"))
                implementation(CLI_LIB)
            }
        }
        commonTest {
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
        if (buildMacX86) {
            val macosX64Main by getting {
                dependsOn(desktopMain)
            }
        }
        if (buildMacArm) {
            val macosArm64Main by getting {
                dependsOn(desktopMain)
            }
        }
        if (buildLinux) {
            val linuxX64Main by getting {
                dependsOn(desktopMain)
            }
        }
        if (buildJvm) {
            val jvmMain by getting {
                dependsOn(commonMain)
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
        entryPoint = "community.flock.wirespec.compiler.cli.main"
    }
}

fun KotlinJsTargetDsl.build() {
    nodejs()
    binaries.executable()
}
