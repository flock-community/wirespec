plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.resources)
    kotlin("plugin.serialization") version "2.0.0-RC1"
}

group = "${libs.versions.group.id.get()}.converter"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    maven(uri("https://s01.oss.sonatype.org/service/local/repo_groups/public/content"))
}

kotlin {
    macosX64()
    macosArm64()
    linuxX64()
    js(IR) {
        nodejs()
    }
    jvm {
        withJava()
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.6.0")
                implementation(project(":src:compiler:core"))
                implementation(libs.kotlinx.serialization)
                implementation(libs.kotlinx.openapi.bindings)
                implementation("io.github.pdvrieze.xmlutil:core:0.86.3")
                implementation("io.github.pdvrieze.xmlutil:serialization:0.86.3")
                implementation("io.github.pdvrieze.xmlutil:serialutil:0.86.3")
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlinx.resources)
                implementation(libs.kotlin.test)
                implementation(libs.bundles.kotest)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.bundles.jackson)
                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.17.1")
            }
        }
    }
}
