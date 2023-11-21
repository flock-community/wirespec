plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.github.johnrengelman.shadow") apply false
}

group = "${Settings.GROUP_ID}.compiler"
version = Settings.version

repositories {
    mavenCentral()
}

kotlin {
    js {
        nodejs()
        generateTypeScriptDefinitions()
        binaries.library()
        compilations["main"].packageJson {
            customField("name", "@flock/wirespec")
            customField("bin", mapOf("wirespec" to "wirespec-bin.js"))
        }
    }
    jvm {
        withJava()
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":src:compiler:cli"))
                implementation(project(":src:compiler:core"))
                implementation(project(":src:openapi"))
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
        }
    }
}
