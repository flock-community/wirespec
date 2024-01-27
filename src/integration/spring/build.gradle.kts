import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.github.johnrengelman.shadow") apply false
    `maven-publish`
    id("org.springframework.boot") version "3.1.3"
    id("io.spring.dependency-management") version "1.1.3"
}

group = "${Settings.GROUP_ID}.integration"
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
        val jvmMain by getting {
            dependencies {
                compileOnly(project(":src:integration:wirespec"))
                implementation(project(":src:integration:jackson"))
                implementation("org.springframework.boot:spring-boot-starter-web")
                implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
                implementation("org.jetbrains.kotlin:kotlin-reflect")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:converter:openapi"))
                implementation(project(":src:integration:wirespec"))
                implementation("org.springframework.boot:spring-boot-starter-test")
            }
        }

    }
}
