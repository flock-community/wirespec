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
                // Full reflection: KClass.constructors / KFunction.call in
                // JvmRefinedWrapper and Class → KType conversion in the
                // Java/Scala adapters. Pinned to the Kotlin backwards-
                // compatibility floor so this published integration doesn't
                // force consumers onto a newer Kotlin than they target.
                implementation(libs.kotlin.reflect.compat)
            }
        }
        jvmTest {
            dependencies {
                implementation(project(":src:integration:wirespec"))
                implementation(libs.scala3.library)
            }
        }
    }
}
