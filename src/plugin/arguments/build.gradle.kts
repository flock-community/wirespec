import Versions.JAVA

plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
}

group = "${Settings.GROUP_ID}.plugin.arguments"
version = Settings.version

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
                languageVersion.set(JavaLanguageVersion.of(JAVA))
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
