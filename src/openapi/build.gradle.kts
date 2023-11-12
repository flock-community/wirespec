plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.github.johnrengelman.shadow") apply false
}

group = Settings.GROUP_ID
version = Settings.version

repositories {
    mavenCentral()
    maven(uri("https://s01.oss.sonatype.org/service/local/repo_groups/public/content"))
}

kotlin {
    macosX64()
    macosArm64()
    linuxX64()
    mingwX64()
    js(IR) {
        nodejs()
    }
    jvm {
        withJava()
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation("community.flock.kotlinx.openapi.bindings:kotlin-openapi-bindings:0.0.19")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit"))
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

