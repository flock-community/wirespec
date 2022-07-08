plugins {
    kotlin("multiplatform") apply false
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") apply false
}

group = "${Settings.groupId}.lsp"
version = Settings.version

repositories {
    mavenCentral()
}

dependencies {
    implementLSP()
    implementTesting()
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
