plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
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
                // KotestDslExtension builds IR File nodes over the parser AST and
                // plugs into the IrEmitter pipeline as an IrExtension. Both compiler
                // modules are multiplatform and the extension uses no JVM APIs, so it
                // lives in commonMain.
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:ir"))
                // wirespec's Kotlin runtime (the Wirespec.* interfaces) is now
                // commonMain, so the framework-neutral context/ambient types that
                // only reference those interfaces live in commonMain too. The serde
                // defaults it also ships stay JVM-only but aren't used here.
                implementation(project(":src:integration:wirespec"))
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
                // The reflection-driven runtime (ArbReceiver, EndpointReflection,
                // CallExecutor, the *CallBuilder terminals) that adapts the commonMain
                // KotestGenerator into a Wirespec.Generator for IR-emitted callers.
                // wirespec itself is inherited transitively from commonMain.
                // The scenario-DSL runtime (WirespecExtension + the generate.request { }
                // entry points) is a kotest framework extension, so the published JVM
                // artifact carries the framework engine and coroutines it builds on.
                implementation(libs.kotest.engine)
                implementation(libs.kotlinx.coroutines.core)
                // Full reflection: KClass.constructors / KFunction.call in
                // JvmRefinedWrapper. Pinned to the Kotlin backwards-compatibility
                // floor so this published integration doesn't force consumers onto
                // a newer Kotlin than they target.
                implementation(libs.kotlin.reflect.compat)
            }
        }
        jvmTest {
            dependencies {
                implementation(project(":src:integration:wirespec"))
                // KotestDslExtension smoke test: compile a .ws fixture through
                // KotlinIrEmitter + the extension and assert the emitted DSL files.
                implementation(project(":src:compiler:emitters:kotlin"))
                implementation(project(":src:compiler:test"))
                implementation(libs.bundles.kotest)
            }
        }
    }
}
