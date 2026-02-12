plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotest)
}

group = "${libs.versions.group.id.get()}.language"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

base.archivesName.set("language-core")

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

    sourceSets.all {
        languageSettings.apply {
            languageVersion = libs.versions.kotlin.compiler.get()
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.io.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}
