import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.process.CommandLineArgumentProvider

plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
    `scala-base`
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.api.get()))
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.language.get()))
    }
    jvm {
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets {
        commonTest {
            dependencies {
                implementation(libs.bundles.kotlin.test)
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.bundles.jackson2)
                // Compile-only so consumers of this module aren't forced onto
                // the Scala runtime; only Scala-emitted user code needs it on
                // its own classpath (the kotest integration brings it in test
                // scope for the Scala-adapter coverage).
                compileOnly("org.scala-lang:scala3-library_3:3.3.4")
            }
        }
    }
}

// `scala-base` attaches a Scala compile to every KMP JVM source set as soon as
// it sees a `src/<sourceSet>/scala` directory. We use those auto-generated
// tasks to compile the emitter's --emit-shared `Wirespec.scala` (so consumers
// of this module find `community.flock.wirespec.scala.Wirespec` on the
// classpath alongside the Java and Kotlin variants), but the Scala tool
// classpath has to be wired in by hand — the full `scala` plugin would do this
// automatically, but it pulls in `java`, which Kotlin Multiplatform rejects.
val scalaToolClasspath: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

// Dedicated tool classpath for running the Wirespec runtime generator. Kept
// isolated from the module's compile/runtime classpaths because the three
// emitters and `:src:compiler:test` are build-time tooling and must never
// leak onto consumers.
val emitterRuntimeClasspath: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    scalaToolClasspath("org.scala-lang:scala3-compiler_3:3.3.4")
    scalaToolClasspath("org.scala-lang:scala3-sbt-bridge:3.3.4")

    emitterRuntimeClasspath(project(":src:compiler:test"))
    emitterRuntimeClasspath(project(":src:compiler:emitters:java"))
    emitterRuntimeClasspath(project(":src:compiler:emitters:kotlin"))
    emitterRuntimeClasspath(project(":src:compiler:emitters:scala"))
    emitterRuntimeClasspath(libs.kotlin.reflect)
}

tasks.withType<ScalaCompile>().configureEach {
    scalaClasspath = scalaToolClasspath
}

// Per-language output dirs so that the Scala compiler's joint-compilation pass
// does not pick up the generated `Wirespec.java` (which would duplicate it with
// the Java compiler's output and break the jar).
val generatedJavaDir = layout.buildDirectory.dir("generated/wirespec-runtime/java")
val generatedKotlinDir = layout.buildDirectory.dir("generated/wirespec-runtime/kotlin")
val generatedScalaDir = layout.buildDirectory.dir("generated/wirespec-runtime/scala")

// Regenerates Wirespec.{java,kt,scala} from the JavaIrEmitter / KotlinIrEmitter
// / ScalaIrEmitter `emitShared()` outputs so the runtime base library is always
// in sync with the emitters. Wired as a source generator for the three JVM
// language source sets below.
val generateWirespecRuntime = tasks.register<JavaExec>("generateWirespecRuntime") {
    description = "Generate Wirespec.{java,kt,scala} runtime base from the Ir emitters."
    group = "build"

    classpath = files(provider { emitterRuntimeClasspath })
    mainClass.set("community.flock.wirespec.compiler.test.WirespecRuntimeGenerator")

    val javaOut = generatedJavaDir
    val kotlinOut = generatedKotlinDir
    val scalaOut = generatedScalaDir

    inputs.files(emitterRuntimeClasspath).withPropertyName("emitterClasspath")
    outputs.dir(javaOut).withPropertyName("generatedJavaSources")
    outputs.dir(kotlinOut).withPropertyName("generatedKotlinSources")
    outputs.dir(scalaOut).withPropertyName("generatedScalaSources")
    argumentProviders.add(
        CommandLineArgumentProvider {
            listOf(
                javaOut.get().asFile.absolutePath,
                kotlinOut.get().asFile.absolutePath,
                scalaOut.get().asFile.absolutePath,
            )
        },
    )
}

kotlin.sourceSets.named("jvmMain") {
    kotlin.srcDir(files(generatedKotlinDir).builtBy(generateWirespecRuntime))
}

sourceSets.named("jvmMain") {
    java.srcDir(files(generatedJavaDir).builtBy(generateWirespecRuntime))
    val scalaSources = (this as ExtensionAware).extensions.getByName("scala") as SourceDirectorySet
    scalaSources.srcDir(files(generatedScalaDir).builtBy(generateWirespecRuntime))
}

tasks.named<Jar>("jvmJar") {
    val jvmMainScala = tasks.named<ScalaCompile>("compileJvmMainScala")
    dependsOn(jvmMainScala)
    from(jvmMainScala.flatMap { it.destinationDirectory })
}
