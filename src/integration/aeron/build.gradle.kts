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
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.api.get()))
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.language.get()))
    }
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            // Agrona (the Aeron buffer library) reaches into jdk.internal.misc.Unsafe.
            jvmArgs(
                "--add-opens",
                "java.base/jdk.internal.misc=ALL-UNNAMED",
                "--add-opens",
                "java.base/sun.nio.ch=ALL-UNNAMED",
            )
        }
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets {
        jvmMain {
            dependencies {
                api(project(":src:integration:wirespec"))
                api(libs.aeron.client)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.aeron.driver)
                implementation(libs.bundles.jackson2)
                implementation(libs.kotlin.junit)
                runtimeOnly(libs.junit.launcher)
            }
        }
    }
}
