import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.scala.ScalaCompile

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

val scalaVersion = "3.3.4"
val generatedWirespecDir = layout.buildDirectory.dir("generated/sources/wirespec")
val wirespecTestSourcesDir = layout.projectDirectory.dir("src/jvmCodegen/resources/wirespec")

kotlin {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.api.get()))
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.language.get()))
    }
    jvm {
        // Private build-time compilation that hosts the Wirespec emitter Main. Kept out
        // of the published artifact, used only by the generateWirespecTestSources task.
        val main = compilations.getByName("main")
        compilations.create("codegen") {
            associateWith(main)
            defaultSourceSet.dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:emitters:java"))
                implementation(project(":src:compiler:emitters:kotlin"))
                implementation(project(":src:compiler:emitters:scala"))
                implementation(libs.kotlin.stdlib)
            }
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets {
        jvmMain {
            dependencies {
                api(project(":src:integration:wirespec"))
                implementation(libs.kotlinx.coroutines.core)
                // The BuilderExtension only reshapes the IR; the compiler comes from the
                // Wirespec plugin that loads it, so it is not forced onto runtime consumers.
                compileOnly(project(":src:compiler:core"))
            }
        }
        jvmTest {
            kotlin.srcDir(generatedWirespecDir.map { it.dir("kotlin") })
            // Kotlin tests drive the generated Java builders too, so the compiler must see the Java sources.
            kotlin.srcDir(generatedWirespecDir.map { it.dir("java") })
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:emitters:java"))
                implementation(project(":src:compiler:emitters:kotlin"))
                implementation(libs.kotlin.junit)
                implementation("org.scala-lang:scala3-library_3:$scalaVersion")
                runtimeOnly(libs.junit.launcher)
            }
        }
    }
}

val codegenCompilation = kotlin.jvm().compilations.named("codegen")

val generateWirespecTestSources = tasks.register<JavaExec>("generateWirespecTestSources") {
    group = "build"
    description = "Generate Java + Kotlin Wirespec test sources with builders from .ws files in src/jvmCodegen/resources/wirespec."
    val compilation = codegenCompilation.get()
    classpath = files(compilation.output.classesDirs) + compilation.runtimeDependencyFiles
    mainClass.set("community.flock.wirespec.integration.jvm.codegen.MainKt")
    val inDir = wirespecTestSourcesDir
    val outDir = generatedWirespecDir
    inputs.dir(inDir).withPropertyName("wirespecSources")
    outputs.dir(outDir).withPropertyName("generatedSources")
    val inputAbsolutePath = inDir.asFile.absolutePath
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                inputAbsolutePath,
                outDir.get().asFile.absolutePath,
                "community.flock.wirespec.integration.jvm",
            )
        },
    )
    dependsOn(compilation.compileTaskProvider)
}

tasks.named<JavaCompile>("compileJvmTestJava") {
    source(generatedWirespecDir.map { it.dir("java") })
    dependsOn(generateWirespecTestSources)
}

tasks.named("compileTestKotlinJvm") {
    dependsOn(generateWirespecTestSources)
}

// `scala-base` attaches a Scala compile to the jvmTest source set because `src/jvmTest/scala`
// exists; it runs after the Kotlin test compile, so Kotlin tests reach the generated Scala
// builders through the checked-in Scala sources at runtime. The full `scala` plugin would wire
// the tool classpath itself, but it pulls in `java`, which Kotlin Multiplatform rejects.
val scalaToolClasspath = configurations.create("scalaToolClasspath") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    scalaToolClasspath("org.scala-lang:scala3-compiler_3:$scalaVersion")
    scalaToolClasspath("org.scala-lang:scala3-sbt-bridge:$scalaVersion")
}

tasks.withType<ScalaCompile>().configureEach {
    scalaClasspath = scalaToolClasspath
}

sourceSets.named("jvmTest") {
    val scalaSources = (this as ExtensionAware).extensions.getByName("scala") as SourceDirectorySet
    scalaSources.srcDir(generatedWirespecDir.map { it.dir("scala") })
}

tasks.named<ScalaCompile>("compileJvmTestScala") {
    dependsOn(generateWirespecTestSources)
}

tasks.named<Test>("jvmTest") {
    val scalaTestCompile = tasks.named<ScalaCompile>("compileJvmTestScala")
    classpath += files(scalaTestCompile.flatMap { it.destinationDirectory })
}
