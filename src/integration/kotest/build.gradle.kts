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
    explicitApi()

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
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:ir"))
                implementation(project(":src:integration:wirespec"))
                implementation(libs.kotest.engine)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlin.reflect.compat)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":src:compiler:emitters:kotlin"))
                implementation(project(":src:compiler:test"))
                implementation(libs.bundles.kotest)
            }
        }
    }
}
