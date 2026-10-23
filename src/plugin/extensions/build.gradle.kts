plugins {
    id("module.publication")
    id("module.spotless")
    id("module.native-targets")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

group = "${libs.versions.group.id.get()}.plugin.extensions"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

// The IR extensions live in the integration modules, which only target the JVM. Their sources are
// compiled here once more, for every target, so the plugins, the CLI and the npm package bundle them.
val integrationExtensionSources = tasks.register<Sync>("syncIntegrationExtensionSources") {
    description = "Collects the IR extension sources of the integration modules."
    from(layout.projectDirectory.dir("../../integration")) {
        include("avro/src/commonMain/kotlin/**/extension/*.kt")
        include("jackson/src/commonMain/kotlin/**/extension/*.kt")
        include("jvm/src/jvmMain/kotlin/**/extension/BuilderExtension.kt")
        include("kotest/src/commonMain/kotlin/**/extension/KotestDslExtension.kt")
        include("kotest/src/commonMain/kotlin/**/convert/*.kt")
        include("kotlinx-serialization/src/commonMain/kotlin/**/extension/*.kt")
        include("spring/src/commonMain/kotlin/**/extension/*.kt")
    }
    into(layout.buildDirectory.dir("generated/sources/extensions"))
}

kotlin {
    js(IR) {
        nodejs()
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
            kotlin.srcDir(integrationExtensionSources)
            dependencies {
                api(project(":src:compiler:core"))
                implementation(project(":src:converter:avro"))
                implementation(libs.kotlinx.serialization)
            }
        }
    }
}
