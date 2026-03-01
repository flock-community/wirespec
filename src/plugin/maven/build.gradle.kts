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

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            artifactId = "wirespec-maven-plugin"
            from(components["java"])
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
tasks.named<org.gradlex.maven.plugin.development.task.GenerateMavenPluginDescriptorTask>("generateMavenPluginDescriptor") {
    upstreamProjects.set(emptyList())
}
