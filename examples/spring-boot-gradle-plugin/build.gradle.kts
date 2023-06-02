import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.4"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.spring") version "1.8.21"
    id("community.flock.wirespec.plugin.gradle") version Settings.version
}

group = "community.flock.wirespec.example-gradle_plugin"
version = Settings.version

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
            srcDir("${buildDir}/generated")
        }
    }
}

wirespec {
    sourceDirectory = "$projectDir/src/main/wirespec"
    kotlin {
        targetDir = "$buildDir/generated/wirespec"
    }
    typescript {
        targetDir = "$projectDir/src/main/frontend/generated"
    }
}

tasks.build {
    dependsOn("wirespec")
}
