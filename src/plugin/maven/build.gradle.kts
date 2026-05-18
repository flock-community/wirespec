plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.plugin.development)
}

group = "${libs.versions.group.id.get()}.plugin.maven"
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
    implementation(project(":src:converter:avro"))
    implementation(project(":src:converter:openapi"))
    implementation(project(":src:plugin:arguments"))
    implementation(libs.kotlin.reflect)
    implementation(libs.bundles.maven.plugin)
    implementation(libs.kotlin.compiler.embeddable)
}

java {
    withSourcesJar()
}

// The `module.publication` convention (vanniktech) auto-registers a `maven`
// publication from `components["java"]`; we only override its artifactId here
// instead of registering a duplicate publication. Registering a second
// publication (e.g. `mavenJava`) would produce two signing tasks writing to
// the same `.asc` path under `build/libs/` and fail Gradle 9's dependency
// validation when the publish task picks up an `.asc` from a sibling sign
// task. `configureEach` is used because vanniktech registers `maven` lazily.
publishing {
    publications.withType<MavenPublication>().configureEach {
        if (name == "maven") {
            artifactId = "wirespec-maven-plugin"
        }
    }
}

tasks.publishToMavenLocal {
    dependsOn(":src:compiler:core:publishToMavenLocal")
    dependsOn(":src:converter:openapi:publishToMavenLocal")
    dependsOn(":src:plugin:arguments:publishToMavenLocal")
}

mavenPlugin {
    artifactId.set("wirespec-maven-plugin")
    description.set("Plugin to run wirespec compiler")
    goalPrefix.set("wirespec")
}

// Workaround: gradlex maven-plugin-development 1.0.3 does not resolve sourceDirectories
// for Kotlin Multiplatform upstream projects, causing Gradle 9 validation errors.
// Since our Mojos don't extend base classes from upstream projects, we can safely clear this.
// Additionally, the plugin only wires Java class/source directories for mojo scanning.
// Since our Mojos are written in Kotlin, we must add the Kotlin output directories explicitly.
tasks.named<org.gradlex.maven.plugin.development.task.GenerateMavenPluginDescriptorTask>("generateMavenPluginDescriptor") {
    upstreamProjects.set(emptyList())
    classesDirs.from(sourceSets.main.map { it.kotlin.classesDirectory })
    sourcesDirs.from(sourceSets.main.map { it.kotlin.sourceDirectories })
}
