plugins {
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

group = "${libs.versions.group.id.get()}.lsp"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    js(IR) {
        nodejs()
        useEsModules()
        binaries.executable()
        compilations["main"].packageJson {
            customField("name", "@flock/wirespec-lsp")
            customField(
                "description",
                "Wirespec Language Server Protocol implementation. Editor-agnostic; runs on Node.",
            )
            customField("bin", mapOf("wirespec-lsp" to "wirespec-lsp.mjs"))
            customField("repository", mapOf("type" to "git", "url" to "https://github.com/flock-community/wirespec"))
            customField("license", "Apache-2.0")
        }
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
                implementation(libs.kotlin.stdlib)
                implementation(libs.arrow.core)
                implementation(libs.kotlinx.serialization)
                implementation(project(":src:compiler:core"))
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        jsMain {
            dependencies {}
        }
        jvmMain {
            dependencies {}
        }
    }
}

tasks.named<Jar>("jvmJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes("Main-Class" to "community.flock.wirespec.lsp.WirespecLsp")
    }
    val jvmRuntimeJarTrees = configurations.named("jvmRuntimeClasspath").get()
        .files
        .filter { it.name.endsWith("jar") }
        .map { zipTree(it) }
    from(jvmRuntimeJarTrees)
}
