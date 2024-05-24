plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
}

group = "${libs.versions.group.id.get()}.plugin.arguments"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

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
            }
        }
    }
}
