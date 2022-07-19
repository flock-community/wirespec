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
    macosX64()
    linuxX64()
    mingwX64()
    js(IR) {
        nodejs()
        binaries.library()
    }
    jvm()
}
