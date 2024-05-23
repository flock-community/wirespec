import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "2.0.0"
    id("io.ktor.plugin") version "2.3.9"
    id("community.flock.wirespec.plugin.gradle") version "0.0.0-SNAPSHOT"
}

group = "community.flock.wirespec.example-gradle_plugin"
version = "0.0.0-SNAPSHOT"

application {
    mainClass.set("community.flock.wirespec.examples.app.ApplicationKt")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-jackson:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

tasks.withType<KotlinCompile> {
    dependsOn("wirespec")
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

sourceSets {
    main {
        java {
            srcDir("${layout.buildDirectory.get()}/generated")
        }
    }
}

wirespec {
    input = "$projectDir/src/main/wirespec"
    kotlin {
        packageName = "community.flock.wirespec.generated.kotlin"
        output = "${layout.buildDirectory.get()}/generated"
        decorators {
            declaration = "@kotlinx.serialization.Serializable"
        }
    }
}
