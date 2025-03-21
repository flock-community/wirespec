plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        withJava()
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets {
        commonTest {
            dependencies {
                implementation(project(":src:integration:wirespec"))
            }
        }
        jvmMain {
            dependencies {
                compileOnly(project(":src:compiler:core"))
                api(project(":src:integration:wirespec"))
                implementation(project(":src:integration:jackson"))
                implementation(libs.jackson.kotlin)
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlinx.coroutines.reactor)
                implementation(libs.spring.boot.web)
                implementation(libs.spring.webflux)
                runtimeOnly(libs.junit.launcher)
            }
        }
        jvmTest {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:converter:openapi"))
                implementation(project(":src:integration:wirespec"))
                implementation(libs.spring.boot.test)
                implementation(libs.kotlin.junit)
                implementation(libs.kotlinx.io.core)
            }
        }
    }
}
