plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "${libs.versions.group.id.get()}.converter"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
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
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(libs.kotlinx.serialization)
                implementation(libs.kotlinx.openapi.bindings)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlinx.io.core)
                implementation(libs.kotlin.test)
                implementation(libs.bundles.kotest)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.bundles.jackson)
            }
        }
    }
}
