plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
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
                implementation(libs.kotlin.test)
                implementation(libs.bundles.kotest)
                implementation(project(":src:integration:wirespec"))
                implementation(project(":src:compiler:test"))
                implementation(project(":src:compiler:core"))
            }
        }
        jvmMain {
            dependencies {
                compileOnly(project(":src:compiler:core"))
                compileOnly(project(":src:compiler:emitters:kotlin"))
                compileOnly(project(":src:compiler:emitters:java"))
                api(project(":src:integration:wirespec"))
                implementation(project(":src:integration:jackson"))
                implementation(libs.bundles.jackson)
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
                implementation(libs.wiremock)
            }
        }
    }
}
