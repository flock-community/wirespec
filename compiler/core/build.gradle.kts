import Libraries.`arrow-core`
import Libraries.`kotlin-coroutines`

plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.github.johnrengelman.shadow") apply false
    id("maven-publish")
}

group = "${Settings.groupId}.compiler"
version = Settings.version

repositories {
    mavenCentral()
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
    }
    sourceSets {
        commonMain {
            dependencies {
                api(`arrow-core`)
                api(`kotlin-coroutines`)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit"))
            }
        }
    }
}

publishing {
    repositories {
        maven {
            credentials {
                username = System.getenv("JFROG_USERNAME")
                password = System.getenv("JFROG_TOKEN")
            }
            name = "flock-maven"
            url = uri("https://flock.jfrog.io/artifactory/flock-maven")
        }
    }
}
