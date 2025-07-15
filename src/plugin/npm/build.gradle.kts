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
            customField("bin", mapOf("wirespec" to "wirespec-bin.mjs"))
            customField(
                "description",
                "Simplify your API development workflows, accelerate implementation, and guarantee strict adherence " +
                    "to defined contract specifications",
            )
            customField(
                "exports",
                mapOf(
                    "." to mapOf(
                        "types" to "./wirespec-src-plugin-npm.d.ts",
                        "default" to "./wirespec-src-plugin-npm.mjs",
                    ),
                    "./fetch" to mapOf(
                        "types" to "./wirespec-fetch.d.ts",
                        "default" to "./wirespec-fetch.mjs",
                    ),
                ),
            )
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
                implementation(project(":src:plugin:arguments"))
                implementation(project(":src:plugin:cli"))
                implementation(project(":src:converter:openapi"))
                implementation(project(":src:converter:avro"))
                implementation(project(":src:tools:generator"))
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
    into(project.buildDir.resolve("dist/js/productionLibrary"))
}

tasks.named("jsProcessResources") {
    dependsOn(copyReadme)
}
