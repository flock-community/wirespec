plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotest)
}

group = "${libs.versions.group.id.get()}.compiler"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    macosX64()
    macosArm64()
    linuxX64()
    js(IR) {
        nodejs()
        useEsModules()
    }
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
        commonMain {
            dependencies {
                api(libs.kotlin.stdlib)
                api(libs.arrow.core)
                implementation(libs.kotlinx.serialization)
                implementation(libs.kotlinx.openapi.bindings)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.bundles.kotest)
            }
        }
    }
}
