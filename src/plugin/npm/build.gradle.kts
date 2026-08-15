plugins {
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
}

group = "${libs.versions.group.id.get()}.plugin.npm"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    js(IR) {
        nodejs()
        useEsModules()
        generateTypeScriptDefinitions()
        binaries.library()
        compilations["main"].packageJson {
            customField("name", "@flock/wirespec")
            customField(
                "bin",
                mapOf(
                    "wirespec" to "wirespec-bin.mjs",
                    "wirespec-lsp" to "wirespec-lsp.mjs",
                ),
            )
            customField(
                "description",
                "Simplify your API development workflows, accelerate implementation, and guarantee strict adherence " +
                    "to defined contract specifications",
            )
            customField(
                "exports",
                mapOf(
                    "." to mapOf(
                        "types" to "./wirespec-src-plugin-npm.d.mts",
                        "default" to "./wirespec-src-plugin-npm.mjs",
                    ),
                    "./fetch" to mapOf(
                        "types" to "./wirespec-fetch.d.ts",
                        "default" to "./wirespec-fetch.mjs",
                    ),
                    "./serialization" to mapOf(
                        "types" to "./wirespec-serialization.d.ts",
                        "default" to "./wirespec-serialization.mjs",
                    ),
                    "./generator" to mapOf(
                        "types" to "./wirespec-generator.d.ts",
                        "default" to "./wirespec-generator.mjs",
                    ),
                    "./msw" to mapOf(
                        "types" to "./wirespec-msw.d.ts",
                        "default" to "./wirespec-msw.mjs",
                    ),
                ),
            )
            customField("peerDependencies", mapOf("msw" to "^2.0.0"))
            customField("peerDependenciesMeta", mapOf("msw" to mapOf("optional" to true)))
            customField("repository", mapOf("type" to "git", "url" to "https://github.com/flock-community/wirespec"))
            customField("license", "Apache-2.0")
        }
    }

    sourceSets.all {
        languageSettings.apply {
            languageVersion = libs.versions.kotlin.compiler.get()
        }
    }

    sourceSets {
        jsMain {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:lib"))
                implementation(project(":src:ide:lsp"))
                implementation(project(":src:plugin:arguments"))
                implementation(project(":src:plugin:cli"))
                implementation(project(":src:converter:openapi"))
                implementation(project(":src:converter:avro"))
                implementation(project(":src:tools:generator"))
                // :src:integration:wirespec is JVM-only (Kotlin 1.9 metadata
                // pin for downstream binary compat). The npm bundle reaches
                // its kotest-based generator types via :src:integration:kotest
                // alone — kotest's commonMain mirrors the field hierarchy.
                implementation(project(":src:integration:kotest"))
                implementation(libs.kotlinx.openapi.bindings)
                implementation(libs.kotlinx.serialization)
            }
        }
        jsTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.io.core)
            }
        }
    }
}

val copyReadme = tasks.register<Copy>("copyReadme") {
    from(project.file("README.md"))
    into(project.layout.buildDirectory.dir("dist/js/productionLibrary"))
}

tasks.named("jsProcessResources") {
    dependsOn(copyReadme)
}
