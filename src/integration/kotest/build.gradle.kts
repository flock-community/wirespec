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
    js(IR) {
        nodejs()
        useEsModules()
    }
    jvm {
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets.all {
        languageSettings.apply {
            languageVersion = libs.versions.kotlin.compiler.get()
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotest.property)
                implementation(libs.kotest.property.arbs)
                implementation(libs.kotlinx.rgxgen)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotest.property)
                implementation(kotlin("test"))
            }
        }
        jvmMain {
            dependencies {
                // wirespec is JVM-only (pinned to Kotlin 1.9 metadata for
                // backward compat with older consumers); the kotest JVM
                // facade adapts the commonMain KotestGenerator into a
                // Wirespec.Generator for IR-emitted callers.
                implementation(project(":src:integration:wirespec"))
            }
        }
        jvmTest {
            dependencies {
                implementation(project(":src:integration:wirespec"))
                implementation("org.scala-lang:scala3-library_3:3.3.4")
            }
        }
    }
}
