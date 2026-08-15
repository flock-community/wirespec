import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.integration.spring.extension.SpringMappingAnnotationsExtension
import community.flock.wirespec.integration.spring.extension.SpringNativeHintsExtension
import community.flock.wirespec.plugin.gradle.CompileWirespecTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.wirespec)
}

group = "community.flock.wirespec.examples.spring"
version = libs.versions.wirespec.get()

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

repositories {
    mavenCentral()
    mavenLocal()
}

configurations.all {
    exclude(group = "community.flock.wirespec.integration", module = "wirespec-jvm")
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.jackson.module.kotlin)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.wirespec.integration.kotest)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.property.arbs)
}

buildscript {
    dependencies {
        classpath(libs.wirespec.compiler)
        classpath(libs.wirespec.emitters.kotlin)
        classpath(libs.wirespec.integration.spring)
    }
}

tasks.register<CompileWirespecTask>("wirespec-kotlin") {
    description = "Compile Wirespec to Kotlin (Spring) with extended shared runtime"
    group = "Wirespec compile"
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.examples.spring.generated"
    emitterClass = KotlinIrEmitter::class.java
    extensionClasses = listOf(
        SpringMappingAnnotationsExtension::class.java,
        SpringNativeHintsExtension::class.java,
    )
    shared = true
}

tasks.withType<KotlinCompile> {
    dependsOn("wirespec-kotlin")
}

sourceSets {
    main {
        kotlin {
            srcDir(layout.buildDirectory.dir("generated"))
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
