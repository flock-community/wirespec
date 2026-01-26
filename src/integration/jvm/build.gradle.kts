plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
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
        jvmMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.jdk8)
            }
        }
        commonMain {
            dependencies {
                compileOnly(project(":src:integration:wirespec"))
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        commonTest {
            dependencies {
                implementation(project(":src:integration:wirespec"))
                implementation(libs.kotlin.junit)
                implementation(libs.mockk)
                implementation(libs.mockito)
            }
        }
    }
}
