import Versions.KOTLIN_COMPILER
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.github.johnrengelman.shadow") apply false
}

group = "${Settings.GROUP_ID}.compiler"
version = Settings.version

repositories {
    mavenCentral()
}

kotlin {
    macosX64 { build() }
    macosArm64 { build() }
    linuxX64 { build() }
    mingwX64 { build() }

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
                api(project(":src:compiler:core"))
                api(project(":src:converter:openapi"))
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
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
    }
}

fun String.fromEnv() = let(System::getenv).toBoolean()

fun KotlinNativeTargetWithHostTests.build() = binaries {
    staticLib {
    }
}

fun KotlinJsTargetDsl.build() {
    nodejs()
//        generateTypeScriptDefinitions()
    binaries.library()
    compilations["main"].packageJson {
        customField("name", "@flock/wirespec")
        customField("bin", mapOf("wirespec" to "wirespec-bin.js"))
    }
}
