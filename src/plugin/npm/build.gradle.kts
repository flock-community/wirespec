plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.resources)
}

group = "${libs.versions.group.id.get()}.plugin.npm"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
}

plugins.withId("maven-publish") {
    tasks.withType(AbstractPublishToMaven::class) {
        logger.info("Disabling $name task in project ${project.name}...")
        enabled = false
    }
}

kotlin {
    js(IR) {
        nodejs()
        generateTypeScriptDefinitions()
        binaries.library()
        compilations["main"].packageJson {
            customField("name", "@flock/wirespec")
            customField("bin", mapOf("wirespec" to "wirespec-bin.js"))
        }
    }

    sourceSets.all {
        languageSettings.apply {
            languageVersion = libs.versions.kotlin.compiler.get()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:lib"))
                implementation(project(":src:plugin:cli"))
                implementation(project(":src:converter:openapi"))
                implementation(project(":src:tools:generator"))
                implementation(libs.kotlinx.openapi.bindings)
                implementation(libs.kotlinx.serialization)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.resources)
            }
        }
    }
}
