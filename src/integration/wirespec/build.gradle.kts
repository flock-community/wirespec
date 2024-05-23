import Versions.JAVA

plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
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
                languageVersion.set(JavaLanguageVersion.of(JAVA))
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {

            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("com.fasterxml.jackson.core:jackson-databind:2.9.8")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
            }
        }
    }
}
