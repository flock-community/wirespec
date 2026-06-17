import org.gradle.api.tasks.scala.ScalaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    js(IR) {
        nodejs()
        useEsModules()
    }
    jvm {
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets.all {
        languageSettings.apply {
            languageVersion = libs.versions.kotlin.compiler.get()
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotest.property)
                implementation(libs.kotlinx.rgxgen)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotest.property)
                implementation(kotlin("test"))
            }
        }
        jvmMain {
            dependencies {
                // wirespec is JVM-only (pinned to Kotlin 1.9 metadata for
                // backward compat with older consumers); the kotest JVM
                // facade adapts the commonMain KotestGenerator into a
                // Wirespec.Generator for IR-emitted callers.
                implementation(project(":src:integration:wirespec"))
                // KotestDslIrExtension builds IR File nodes (file{}/raw) over the
                // parser AST and plugs into the IrEmitter pipeline as an IrExtension.
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:ir"))
                // Full reflection: KClass.constructors / KFunction.call in
                // JvmRefinedWrapper and Class → KType conversion in the
                // Java/Scala adapters. Pinned to the Kotlin backwards-
                // compatibility floor so this published integration doesn't
                // force consumers onto a newer Kotlin than they target.
                implementation(libs.kotlin.reflect.compat)
                // Compile-only: needed by the KotestWirespec Scala facade, but
                // consumers bring their own Scala runtime.
                compileOnly(libs.scala3.library)
            }
        }
        jvmTest {
            dependencies {
                implementation(project(":src:integration:wirespec"))
                implementation(libs.scala3.library)
                // KotestDslIrExtension smoke test: compile a .ws fixture through
                // KotlinIrEmitter + the extension and assert the emitted DSL files.
                implementation(project(":src:compiler:emitters:kotlin"))
                implementation(project(":src:compiler:test"))
                implementation(libs.bundles.kotest)
            }
        }
    }
}

// `scala-base` attaches a Scala compile to every JVM source set that has a
// `src/<sourceSet>/scala` directory (here: the KotestWirespec facade in
// jvmMain and its smoke test in jvmTest), but the Scala tool classpath has to
// be wired in by hand — the full `scala` plugin would do this automatically,
// but it pulls in `java`, which Kotlin Multiplatform rejects.
val scalaToolClasspath: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    scalaToolClasspath(libs.scala3.compiler)
    scalaToolClasspath(libs.scala3.sbt.bridge)
}

tasks.withType<ScalaCompile>().configureEach {
    scalaClasspath = scalaToolClasspath
}

// Unlike :src:integration:wirespec (whose generated Wirespec.scala is
// self-contained), the facade calls Kotlin code, so the Scala compiles need
// the Kotlin outputs and the KMP compile classpaths.
val compileKotlinJvm = tasks.named<KotlinCompile>("compileKotlinJvm")
val compileJvmMainScala = tasks.named<ScalaCompile>("compileJvmMainScala") {
    dependsOn(compileKotlinJvm)
    classpath += files(compileKotlinJvm.flatMap { it.destinationDirectory }) +
        configurations["jvmCompileClasspath"]
}

tasks.named<Jar>("jvmJar") {
    dependsOn(compileJvmMainScala)
    from(compileJvmMainScala.flatMap { it.destinationDirectory })
}

val compileTestKotlinJvm = tasks.named<KotlinCompile>("compileTestKotlinJvm")
val compileJvmTestScala = tasks.named<ScalaCompile>("compileJvmTestScala") {
    dependsOn(compileKotlinJvm, compileTestKotlinJvm, compileJvmMainScala)
    classpath += files(
        compileKotlinJvm.flatMap { it.destinationDirectory },
        compileTestKotlinJvm.flatMap { it.destinationDirectory },
        compileJvmMainScala.flatMap { it.destinationDirectory },
    ) + configurations["jvmTestCompileClasspath"]
}

tasks.named<Test>("jvmTest") {
    dependsOn(compileJvmTestScala)
    testClassesDirs += files(compileJvmTestScala.flatMap { it.destinationDirectory })
    classpath += files(
        compileJvmTestScala.flatMap { it.destinationDirectory },
        compileJvmMainScala.flatMap { it.destinationDirectory },
    )
}
