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
