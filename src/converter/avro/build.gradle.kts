plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.goncalossilva.resources") version "0.4.0"
    kotlin("jvm") apply false
    id("com.github.johnrengelman.shadow") apply false
}

group = "${Settings.GROUP_ID}.converter"
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
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit"))
                implementation("com.goncalossilva:resources:0.4.0")
            }
        }
    }
}
