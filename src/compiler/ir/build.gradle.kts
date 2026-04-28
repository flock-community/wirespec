plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
}

group = "${libs.versions.group.id.get()}.ir"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

val enableNative = (findProperty("wirespec.enableNative") as String?).toBoolean()

kotlin {
    if (enableNative) {
        macosX64()
        macosArm64()
        linuxX64()
        mingwX64()
    }
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

    sourceSets.all {
        languageSettings.apply {
            languageVersion = libs.versions.kotlin.compiler.get()
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.io.core)
                implementation(project(":src:compiler:core"))
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.bundles.kotest)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}
