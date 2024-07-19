plugins {
    kotlin("multiplatform")
    id("com.goncalossilva.resources") version "0.4.0"
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
                implementation(project(":src:compiler:core"))
                implementation("io.github.pdvrieze.xmlutil:core:0.86.3")
                implementation("io.github.pdvrieze.xmlutil:serialization:0.86.3")
                implementation("io.github.pdvrieze.xmlutil:serialutil:0.86.3")
            }
        }
        commonTest {
            dependencies {
                implementation("com.goncalossilva:resources:0.4.0")
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit"))
                implementation(libs.bundles.kotest)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("com.fasterxml.jackson.core:jackson-databind:2.9.8")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
            }
        }
    }
}
