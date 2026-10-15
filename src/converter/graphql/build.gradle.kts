plugins {
    id("module.publication")
    id("module.spotless")
    id("module.native-targets")
    alias(libs.plugins.kotlin.multiplatform)
}

group = "${libs.versions.group.id.get()}.converter"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    js(IR) {
        nodejs()
    }
    jvm {
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                api(project(":src:converter:common"))
            }
        }
        commonTest {
            dependencies {
                implementation(project(":src:compiler:test"))
                implementation(project(":src:compiler:emitters:wirespec"))
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.io.core)
                implementation(libs.bundles.kotest)
            }
        }
    }
}
