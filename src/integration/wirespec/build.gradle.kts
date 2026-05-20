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
        // Pinned to 2.0 — not the project-wide 1.9, because the K2 native metadata
        // compiler rejects 1.9; and not 2.1, because metadata produced at 2.1 cannot
        // be read by the project's existing 1.9-compiler consumers (e.g. examples/gradle-ktor).
        // 2.0 is the floor for native compilation and the ceiling for 1.9-compiler readers.
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
    jvm {
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    // iOS targets enable Kotlin Multiplatform consumers (e.g. Compose Multiplatform mobile
    // apps) to link against the Wirespec runtime. Android consumes the jvm artifact.
    // Only Wirespec.kt (in commonMain) is platform-neutral — serde defaults remain JVM-only
    // because they use Jackson and kotlin.reflect.KClass.java.
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        jvmMain {
            dependencies {
                implementation(libs.bundles.jackson)
            }
        }
        jvmTest {
            // The kotlin.test bundle includes kotlin-test-junit (JVM-only).
            // Kept under jvmTest so iOS test configurations don't try to resolve it.
            dependencies {
                implementation(libs.bundles.kotlin.test)
            }
        }
    }
}
