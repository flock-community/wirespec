import community.flock.wirespec.compiler.core.emit.KotlinLegacyEmitter
import community.flock.wirespec.compiler.core.emit.shared.KotlinShared
import community.flock.wirespec.compiler.core.emit.transformer.ClassModelTransformer.transform
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.plugin.FileExtension
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.gradle.CompileWirespecTask
import community.flock.wirespec.plugin.gradle.CustomWirespecTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.wirespec)
}

group = "community.flock.wirespec.example-gradle_plugin"
version = libs.versions.default.get()

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
    implementation(libs.bundles.ktor)
    implementation(libs.logback)
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.ktor.test)
}

tasks.withType<KotlinCompile> {
    dependsOn("wirespec-kotlin")
    dependsOn("wirespec-typescript")
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

sourceSets {
    main {
        java {
            srcDir("${layout.buildDirectory.get()}/generated")
        }
    }
}

buildscript {
    dependencies {
        classpath(libs.wirespec.compiler)
    }
}

tasks.register<CustomWirespecTask>("wirespec-kotlin") {
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.generated.kotlin"
    emitter = KotlinSerializableEmitter::class.java
    shared = KotlinShared.source
    extension = FileExtension.Kotlin.value
}

tasks.register<CompileWirespecTask>("wirespec-typescript") {
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.kotlin"
    languages = listOf(Language.Kotlin)
}

class KotlinSerializableEmitter : KotlinLegacyEmitter("community.flock.wirespec.generated.kotlin") {

    override fun Type.emit(ast: AST): String = """
        |@kotlinx.serialization.Serializable
        |${transform(ast).emit()}
    """.trimMargin()

    override fun Refined.emit(): String = """
        |@kotlinx.serialization.Serializable
        |${transform().emit()}
    """.trimMargin()
}
