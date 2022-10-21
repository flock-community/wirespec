import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.4"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.spring") version "1.7.10"
    id("community.flock.wirespec.plugin.gradle") version "0.0.1-SNAPSHOT"
}

group = "community.flock.wirespec.example-gradle_plugin"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

sourceSets {
    main {
        java {
            srcDir("${buildDir}/generated/main/kotlin")
        }
    }
}

wirespec {
    sourceDirectory = "$projectDir/src/main/wire-spec"
    kotlin {
        targetDirectory = "$buildDir/generated/main/kotlin"
    }
    typescript {
        targetDirectory = "$projectDir/src/main/frontend/generated"
    }
}

tasks.build {
    dependsOn("wirespec")
}
