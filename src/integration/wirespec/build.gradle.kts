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
                implementation(libs.bundles.jackson)
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

dependencies {
    scalaToolClasspath("org.scala-lang:scala3-compiler_3:3.3.4")
    scalaToolClasspath("org.scala-lang:scala3-sbt-bridge:3.3.4")
}

tasks.withType<ScalaCompile>().configureEach {
    scalaClasspath = scalaToolClasspath
}

tasks.named<Jar>("jvmJar") {
    val jvmMainScala = tasks.named<ScalaCompile>("compileJvmMainScala")
    dependsOn(jvmMainScala)
    from(jvmMainScala.flatMap { it.destinationDirectory })
}
