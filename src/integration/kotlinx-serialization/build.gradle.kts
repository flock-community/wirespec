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
    // Kotlin 2.0 is this module's consumer-compatibility floor, matching the
    // kotlin_libraries floor: the current compiler rejects the repo-wide 1.9
    // floor for the JS/native/metadata compilations this module now has, and a
    // JVM-only 1.9 floor would sit below commonMain's, which KGP forbids.
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
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
