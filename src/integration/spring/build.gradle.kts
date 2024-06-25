import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("multiplatform")
    id("org.springframework.boot") version "3.1.3"
    id("io.spring.dependency-management") version "1.1.3"
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

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
        val jvmMain by getting {
            dependencies {
                compileOnly(project(":src:compiler:core"))
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
