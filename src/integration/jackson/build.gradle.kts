plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
}

kotlin {
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
                compileOnly(project(":src:integration:wirespec"))
            }
        }
        commonTest {
            dependencies {
                implementation(project(":src:integration:wirespec"))
                implementation(libs.bundles.kotlin.test)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(project(":src:compiler:core"))
                compileOnly(libs.bundles.jackson)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.bundles.jackson)
            }
        }
    }
}
