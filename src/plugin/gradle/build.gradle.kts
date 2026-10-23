plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
}

group = "${libs.versions.group.id.get()}.plugin.gradle"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":src:compiler:core"))
    implementation(project(":src:converter:asyncapi"))
    implementation(project(":src:converter:avro"))
    implementation(project(":src:converter:graphql"))
    implementation(project(":src:converter:openapi"))
    implementation(project(":src:plugin:arguments"))
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}

// CI runs `./gradlew jvmTest`; this JVM-only module has no such task otherwise.
tasks.register("jvmTest") {
    group = "verification"
    description = "Runs the tests of the Gradle plugin"
    dependsOn(tasks.test)
}

java {
    withSourcesJar()
}

gradlePlugin {
    plugins.create("kotlin") {
        id = "${libs.versions.group.id.get()}.plugin.gradle"
        implementationClass = "community.flock.wirespec.plugin.gradle.WirespecPlugin"
        displayName = "Wirespec gradle plugin"
        description = "Plugin for compiling Wirespec files"
    }
}

tasks.publishToMavenLocal {
    dependsOn(":src:compiler:core:publishToMavenLocal")
    dependsOn(":src:plugin:arguments:publishToMavenLocal")
    dependsOn(":src:plugin:extensions:publishToMavenLocal")
}
