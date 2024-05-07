plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.goncalossilva.resources") version "0.4.0"
}

group = "${Settings.GROUP_ID}.integration"
version = Settings.version

repositories {
    mavenCentral()
    maven(uri("https://s01.oss.sonatype.org/service/local/repo_groups/public/content"))
}

kotlin {
    jvm {
        withJava()
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                compileOnly(project(":src:integration:wirespec"))
            }
        }
        commonTest {
            dependencies {
                implementation(project(":src:integration:wirespec"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(project(":src:compiler:core"))
                compileOnly("com.fasterxml.jackson.core:jackson-databind:2.16.1")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
            }
        }
    }
}
