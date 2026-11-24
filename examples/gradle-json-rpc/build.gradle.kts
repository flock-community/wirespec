import com.diffplug.gradle.spotless.SpotlessTask
import community.flock.wirespec.integration.jsonrpc.extension.JsonRpcExtension
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.gradle.CompileWirespecTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spotless)
    alias(libs.plugins.wirespec)
}

group = "community.flock.wirespec.example.gradle"
version = libs.versions.wirespec.get()

application {
    mainClass.set("community.flock.wirespec.example.jsonrpc.ApplicationKt")
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // The Wirespec runtime: the shared `Wirespec` the generated code compiles against.
    implementation(libs.wirespec.integration)
    implementation(libs.bundles.ktor)
    implementation(libs.jackson)
    implementation(libs.logback)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.server.test.host)
}

buildscript {
    dependencies {
        classpath(libs.wirespec.compiler)
    }
}

// The JsonRpc extension ships with the plugin, so it needs no dependency of its own.
tasks.register<CompileWirespecTask>("wirespec-kotlin") {
    description = "Compile Wirespec to Kotlin, with a JSON-RPC client and server for the rpcs"
    group = "Wirespec compile"
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.generated"
    language = Language.Kotlin
    extensionClasses = listOf(JsonRpcExtension::class.java)
}

tasks.withType<KotlinCompile> {
    dependsOn("wirespec-kotlin")
}

tasks.withType<SpotlessTask> {
    dependsOn("wirespec-kotlin")
}

sourceSets {
    main {
        java {
            srcDir("${layout.buildDirectory.get()}/generated")
        }
    }
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
        targetExclude("**/build/**")
        ktlint().editorConfigOverride(
            mapOf("ktlint_code_style" to "intellij_idea"),
        )
    }
}
