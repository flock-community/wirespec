import community.flock.wirespec.integration.kotest.extension.KotestDslExtension
import community.flock.wirespec.integration.spring.extension.SpringMappingAnnotationsExtension
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.gradle.CompileWirespecTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.wirespec)
}

group = "community.flock.wirespec.examples"
version = libs.versions.wirespec.get()

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // The Wirespec Spring integration turns the generated `*.Handler` interfaces into
    // Spring `@RestController`s and installs the JSON (de)serialization for the models.
    implementation(libs.wirespec.integration.spring)
    implementation(libs.spring.boot.starter.web)
    // Wirespec handlers are `suspend` functions; Spring MVC dispatches them through Reactor.
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.spring.kafka)

    // `KotestDslExtension` emits the scenario DSL into the *main* source set next to the
    // models, so the main compilation needs the DSL runtime and kotest-property to compile.
    testImplementation(libs.kotest.property)
    testImplementation(libs.wirespec.integration.kotest)

    // Tests build a `Wirespec.Serialization` from Jackson to drive the generated client.
    testImplementation(libs.wirespec.integration.jackson)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.extensions.spring)
    // In-JVM Kafka broker for the channel scenarios (`@EmbeddedKafka`), no Docker needed.
    testImplementation(libs.spring.kafka.test)
    // WireMock backs the mock server the response-side scenario DSL (`.mock { req -> … }`) drives;
    // the wirespec WireMock integration supplies the request/response stub builders it reuses.
    testImplementation(libs.wiremock)
    testImplementation(libs.wirespec.integration.wiremock)
}

// The generator itself runs on the buildscript classpath. Besides the compiler and the
// Kotlin emitter it needs the two IR extensions applied below: the Spring one (adds the
// `@GetMapping`/`@PostMapping` annotations) and the Kotest one (adds the scenario DSL).
buildscript {
    dependencies {
        classpath(libs.wirespec.compiler)
        classpath(libs.wirespec.emitters.kotlin)
        classpath(libs.wirespec.integration.spring)
        classpath(libs.wirespec.integration.kotest)
    }
}

tasks.register<CompileWirespecTask>("wirespec-kotlin") {
    description = "Compile Wirespec to Kotlin (Spring controllers + Kotest scenario DSL)"
    group = "Wirespec compile"
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.examples.kotest.generated"
    languages = listOf(Language.Kotlin)
    ir = true
    extensionClasses = listOf(
        SpringMappingAnnotationsExtension::class.java,
        KotestDslExtension::class.java,
    )
}

sourceSets {
    main {
        java {
            srcDir(layout.buildDirectory.dir("generated"))
        }
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("wirespec-kotlin")
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Point Kotest at the project config in this package instead of the default
    // `io.kotest.provided.ProjectConfig`.
    systemProperty("kotest.framework.config.fqn", "community.flock.wirespec.examples.kotest.ProjectConfig")
}
