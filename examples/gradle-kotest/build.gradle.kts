import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.integration.kotest.extension.KotestDslExtension
import community.flock.wirespec.integration.spring.extension.SpringMappingAnnotationsExtension
import community.flock.wirespec.plugin.gradle.CompileWirespecTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.wirespec)
}

group = "community.flock.wirespec.examples.kotest"
version = libs.versions.wirespec.get()

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
    // The kotest scenario-DSL runtime reflects the generated Request constructor's
    // parameter names (id, body, …), so they must be retained in the bytecode.
    compilerOptions {
        javaParameters.set(true)
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.kafka)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.reactor)
    // The Spring integration wires the generated handlers into Spring MVC and
    // contributes the `Wirespec.Serialization` bean (Jackson-backed). It also brings
    // the shared `Wirespec` runtime (`wirespec-jvm`), so the codegen runs with
    // `shared = false` to avoid emitting a second copy of that class.
    implementation(libs.wirespec.integration.spring)
    implementation(libs.wirespec.integration.jackson)
    // The Wirespec IR emitter generates a Kotest scenario DSL (`<Op>Dsl.kt`) next to
    // the models; those files import the kotest integration's `*Call` runtime and
    // `io.kotest.property`, so both live on the main classpath of this example.
    implementation(libs.wirespec.integration.kotest)
    implementation(libs.kotest.property)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property.arbs)
    testImplementation(libs.kotlinx.coroutines.test)
}

buildscript {
    dependencies {
        classpath(libs.wirespec.compiler)
        classpath(libs.wirespec.emitters.kotlin)
        classpath(libs.wirespec.integration.spring)
        classpath(libs.wirespec.integration.kotest)
    }
}

tasks.register<CompileWirespecTask>("wirespec-kotlin") {
    description = "Compile Wirespec to Kotlin (Spring) with the Kotest scenario DSL"
    group = "Wirespec compile"
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.examples.kotest.generated"
    emitterClass = KotlinIrEmitter::class.java
    extensionClasses = listOf(
        SpringMappingAnnotationsExtension::class.java,
        KotestDslExtension::class.java,
    )
    // The shared `Wirespec` runtime comes from the `wirespec-jvm` artifact (pulled in
    // by the Spring/Kotest integrations), so it is not emitted into the project here.
    shared = false
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
