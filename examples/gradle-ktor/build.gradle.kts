import com.diffplug.gradle.spotless.SpotlessTask
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.gradle.CompileWirespecTask
import community.flock.wirespec.plugin.gradle.ConvertWirespecTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.spotless)
    alias(libs.plugins.wirespec)
}

group = "community.flock.wirespec.example.gradle"
version = libs.versions.wirespec.get()

application {
    mainClass.set("community.flock.wirespec.examples.app.ApplicationKt")
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(libs.wirespec.integration)
    implementation(libs.bundles.ktor)
    implementation(libs.jackson)
    implementation(libs.logback)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.bundles.ktor.test)
}

tasks.withType<KotlinCompile> {
    dependsOn("wirespec-kotlin")
    dependsOn("wirespec-typescript")
    dependsOn("wirespec-openapi")
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<SpotlessTask> {
    dependsOn("wirespec-kotlin")
    dependsOn("wirespec-typescript")
    dependsOn("wirespec-openapi")
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
        targetExclude("**/build/**", "**/resources/**", "**/*Emitter.kt")
        ktlint().editorConfigOverride(
            mapOf("ktlint_code_style" to "intellij_idea"),
        )
    }
}

buildscript {
    dependencies {
        classpath(libs.wirespec.compiler)
        classpath(libs.wirespec.emitters.kotlin)
    }
}

tasks.register<CompileWirespecTask>("wirespec-kotlin") {
    description = "Compile Wirespec to Kotlin"
    group = "Wirespec compile"
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.generated.kotlin"
    emitterClass = KotlinSerializableEmitter::class.java
}

tasks.register<CompileWirespecTask>("wirespec-typescript") {
    description = "Compile Wirespec to TypeScript"
    group = "Wirespec compile"
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.generated.typescript"
    languages = listOf(Language.TypeScript)
}

tasks.register<ConvertWirespecTask>("wirespec-openapi") {
    description = "Convert OpenApi to Wirespec"
    group = "Wirespec convert"
    input = layout.projectDirectory.file("src/main/openapi/petstorev3.json")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.generated.openapi"
    languages = listOf(Language.Wirespec)
    format = Format.OpenAPIV3
    preProcessor = { it.replaceFirst("http://www.apache.org/licenses/LICENSE-2.0.html", "https://flock.community") }
}

class KotlinSerializableEmitter : KotlinEmitter(PackageName("community.flock.wirespec.generated.kotlin")) {

    override fun emit(type: Type, module: Module): String = """
        |@kotlinx.serialization.Serializable
        |${super.emit(type, module)}
    """.trimMargin()

    override fun emit(refined: Refined): String = """
        |@kotlinx.serialization.Serializable
        |${super.emit(refined)}
    """.trimMargin()
}
