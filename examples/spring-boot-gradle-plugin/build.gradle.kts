import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    id("community.flock.wirespec.plugin.gradle") version "0.0.0-SNAPSHOT"
}

group = "community.flock.wirespec.example-gradle_plugin"
version = "0.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}

sourceSets {
    main {
        java {
            srcDir("${buildDir}/generated")
        }
    }
}

wirespec {
    input = "$projectDir/src/main/wirespec"
    kotlin {
        packageName = "community.flock.wirespec.generated.kotlin"
        output = "$buildDir/generated/community/flock/wirespec/generated/kotlin"
    }
    typescript {
        output = "$projectDir/src/main/typescript/generated"
    }
}

tasks.build {
    dependsOn("wirespec")
}
