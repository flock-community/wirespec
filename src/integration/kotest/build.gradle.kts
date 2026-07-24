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
                // wirespec's Kotlin runtime (the Wirespec.* interfaces), the
                // framework-neutral context/ambient types, and the whole scenario-DSL
                // runtime all live in commonMain. The module has a single jvm() target,
                // so commonMain compiles against the JVM and freely uses reflection
                // (ArbReceiver, EndpointReflection, CallExecutor, the *CallBuilder
                // terminals) alongside the kotest framework extensions.
                implementation(project(":src:integration:wirespec"))
                // The kotest framework extensions (WirespecEndpoint/Channel/MockExtension)
                // build on the engine's TestCaseExtension/AfterSpecListener and coroutines.
                implementation(libs.kotest.engine)
                implementation(libs.kotlinx.coroutines.core)
                // Full reflection: KClass.constructors / KFunction.call in
                // JvmRefinedWrapper. Pinned to the Kotlin backwards-compatibility
                // floor so this published integration doesn't force consumers onto
                // a newer Kotlin than they target.
                implementation(libs.kotlin.reflect.compat)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                // kotest.property and :src:integration:wirespec come transitively from
                // commonMain (commonTest dependsOn commonMain), so they aren't re-declared here.
                // KotestDslExtension smoke test: compile a .ws fixture through
                // KotlinIrEmitter + the extension and assert the emitted DSL files.
                implementation(project(":src:compiler:emitters:kotlin"))
                implementation(project(":src:compiler:test"))
                implementation(libs.bundles.kotest)
            }
        }
    }
}
