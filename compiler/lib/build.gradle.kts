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
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":compiler:core"))
            }
        }
        val jsMain by getting {
            dependsOn(commonMain)
        }
    }
}
