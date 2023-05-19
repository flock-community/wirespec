plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.github.johnrengelman.shadow") apply false
    `maven-publish`
}

group = "${Settings.groupId}.compiler"
version = Settings.version

repositories {
    mavenCentral()
    maven(uri("https://s01.oss.sonatype.org/service/local/repo_groups/public/content"))
}

kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":compiler:core"))
                implementation("community.flock.kotlinx.openapi.bindings:kotlin-openapi-bindings:0.0.8")
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