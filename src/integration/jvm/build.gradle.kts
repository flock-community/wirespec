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
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets {
        jvmMain {
            dependencies {
                // The generated Wirespec runtime (community.flock.wirespec.kotlin.Wirespec)
                // whose Transportation/RawRequest/RawResponse this adapter implements.
                api(project(":src:integration:wirespec"))
                // Dispatchers.IO / withContext used to run the blocking JDK HttpClient send
                // off the calling thread.
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.bundles.kotlin.test)
                implementation(libs.bundles.kotest)
            }
        }
    }
}
