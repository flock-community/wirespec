plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.resources)
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    maven(uri("https://s01.oss.sonatype.org/service/local/repo_groups/public/content"))
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        withJava()
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets {
        commonTest {
            dependencies {
                implementation(project(":src:integration:wirespec"))
            }
        }
        val jvmMain by getting {
            dependencies {
                compileOnly(project(":src:integration:wirespec"))
                compileOnly(project(":src:compiler:core"))
                implementation(project(":src:integration:jackson"))
                implementation("org.springframework.boot:spring-boot-starter-web")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
                implementation("org.jetbrains.kotlin:kotlin-reflect")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
                runtimeOnly("org.junit.platform:junit-platform-launcher")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:converter:openapi"))
                implementation(project(":src:integration:wirespec"))
                implementation("org.springframework.boot:spring-boot-starter-test")
                implementation("org.jetbrains.kotlin:kotlin-test-junit5")
            }
        }
    }
}
