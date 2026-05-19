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
        // The project-wide pin of 1.9 (libs.versions.kotlin.api/language) is JVM-only;
        // the K2 native metadata compiler requires 2.0+. Wirespec.kt uses no 2.x-only
        // language features, so bumping is a no-op for JVM consumers.
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
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
