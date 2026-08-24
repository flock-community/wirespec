import com.diffplug.gradle.spotless.SpotlessTask
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
    alias(libs.plugins.spotless)
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
    implementation(libs.wirespec.integration.spring)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.spring.kafka)

    implementation(libs.kotest.property)
    implementation(libs.wirespec.integration.kotest)
    implementation(libs.wirespec.integration.jvm)

    testImplementation(libs.wirespec.integration.jackson)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.extensions.spring)
    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.wiremock)
    testImplementation(libs.wirespec.integration.wiremock)
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
    description = "Compile Wirespec to Kotlin (Spring controllers + Kotest scenario DSL)"
    group = "Wirespec compile"
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.examples.kotest.generated"
    languages = listOf(Language.Kotlin)
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
    systemProperty("kotest.framework.config.fqn", "community.flock.wirespec.examples.kotest.ProjectConfig")
}

tasks.withType<SpotlessTask> {
    dependsOn("wirespec-kotlin")
}

spotless {
    format("misc") {
        target("**/.gitignore", "**/*.properties", "**/*.md")
        endWithNewline()
    }

    format("wirespec") {
        target("**/*.ws")
        endWithNewline()
    }

    kotlin {
        target("**/*.kt", "**/*.kts")
        targetExclude("**/build/**", "**/resources/**")
        ktlint().editorConfigOverride(
            mapOf("ktlint_code_style" to "intellij_idea"),
        )
    }
}
