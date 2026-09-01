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
        // The 1.9 language/api floor is a JVM-consumer compatibility guarantee; the
        // JS/native/metadata compilations of the current compiler no longer accept 1.9,
        // so the floor applies to the JVM target only.
        compilerOptions {
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.api.get()))
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.language.get()))
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                // The Wirespec runtime and emitter come from the consumer's
                // build; this module only reshapes the IR with raw, fully
                // qualified kotlinx.serialization annotations, so no
                // kotlinx-serialization dependency is needed here. compileOnly
                // keeps the compiler off the JVM consumer's runtime classpath;
                // JS/Native don't support compileOnly, so those source sets
                // redeclare the same modules as api.
                compileOnly(project(":src:compiler:core"))
                compileOnly(project(":src:compiler:ir"))
            }
        }
        jsMain {
            dependencies {
                api(project(":src:compiler:core"))
                api(project(":src:compiler:ir"))
            }
        }
        if (enableNative) {
            nativeMain {
                dependencies {
                    api(project(":src:compiler:core"))
                    api(project(":src:compiler:ir"))
                }
            }
        }
        jvmMain {
            dependencies {
                api(project(":src:integration:wirespec"))
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.bundles.kotest)
                implementation(libs.kotlin.junit)
                implementation(project(":src:integration:wirespec"))
                implementation(project(":src:compiler:test"))
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:emitters:kotlin"))
            }
        }
    }
}
