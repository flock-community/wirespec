import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.scala.ScalaCompile

plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
    id("module.wirespec-codegen")
    `scala-base`
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

wirespecCodegen {
    basePackage = "community.flock.wirespec.integration.jvm"
    languages = listOf("Java", "Kotlin", "Scala")
    irExtensions = listOf("JvmBuilder")
}

val scalaVersion = "3.3.4"

kotlin {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.api.get()))
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.language.get()))
    }
    jvm {
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
                // The JvmBuilderExtension only reshapes the IR; the compiler comes from the
                // Wirespec plugin that loads it, so it is not forced onto runtime consumers.
                compileOnly(project(":src:compiler:core"))
            }
        }
        jvmTest {
            // Kotlin tests drive the generated Java builders too, so the compiler must see the Java sources.
            kotlin.srcDir(wirespecCodegen.outputDir.dir("java"))
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
    scalaSources.srcDir(wirespecCodegen.outputDir.dir("scala"))
}

tasks.named<ScalaCompile>("compileJvmTestScala") {
    dependsOn(tasks.named("generateWirespecTestSources"))
}

tasks.named<Test>("jvmTest") {
    val scalaTestCompile = tasks.named<ScalaCompile>("compileJvmTestScala")
    classpath += files(scalaTestCompile.flatMap { it.destinationDirectory })
}
