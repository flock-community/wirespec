plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
    maven(uri("https://packages.confluent.io/maven"))
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
        jvmMain {
            dependencies {
                compileOnly(project(":src:compiler:core"))
                compileOnly(project(":src:integration:wirespec"))
                compileOnly("io.confluent:kafka-avro-serializer:7.7.1")
                implementation(project(":src:compiler:emitters:kotlin"))
                implementation(project(":src:compiler:emitters:java"))
                implementation(project(":src:converter:avro"))
                implementation(libs.jackson.kotlin)
                implementation(libs.jackson.kotlin)
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlinx.coroutines.reactor)
                implementation(libs.kotlinx.serialization)
                implementation(libs.spring.boot.web)
                runtimeOnly(libs.junit.launcher)
            }
        }
        jvmTest {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:integration:wirespec"))
                implementation("io.confluent:kafka-avro-serializer:7.7.1")
                implementation(libs.spring.boot.test)
                implementation(libs.kotlin.junit)
            }
        }
    }
}
