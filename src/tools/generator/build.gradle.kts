plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "${libs.versions.group.id.get()}.generator"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    maven(uri("https://s01.oss.sonatype.org/service/local/repo_groups/public/content"))
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
                implementation(libs.kotlinx.rgxgen)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}
