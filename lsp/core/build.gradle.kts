plugins {
    kotlin("jvm") version Versions.Languages.kotlin
}

group = "community.flock.wirespec.lsp"
version = "0.0.1-SNAPSHOT"

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
