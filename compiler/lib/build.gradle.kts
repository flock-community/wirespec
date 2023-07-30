plugins {
    kotlin("multiplatform")
    kotlin("jvm") apply false
    id("com.github.johnrengelman.shadow") apply false
}

group = "${Settings.groupId}.compiler"
version = Settings.version

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        nodejs()
        binaries.library()
        compilations["main"].packageJson {
            customField("name", "@flock/wirespec")
            customField("bin", mapOf("wirespec" to "kotlin/wirespec-bin.js"))
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":compiler:cli"))
                implementation(project(":compiler:core"))
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
        }
    }
}
