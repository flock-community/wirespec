import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.github.johnrengelman.shadow") apply false
    `maven-publish`
    id("org.springframework.boot") version "3.1.3"
    id("io.spring.dependency-management") version "1.1.3"
}

group = "${Settings.GROUP_ID}.integration.spring"
version = Settings.version

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        withJava()
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        tasks.getByName<BootJar>("bootJar") {
            enabled = false
        }

        tasks.getByName<Jar>("jar") {
            enabled = true
            exclude("community/flock/wirespec/Wirespec.class")
        }

        val jvmMain by getting {
            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-web")
                implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
                implementation("org.jetbrains.kotlin:kotlin-reflect")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
                implementation("org.springframework.boot:spring-boot-starter-test")
            }
        }

    }
}
