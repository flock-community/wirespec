plugins {
    id("module.spotless")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij)
}

group = "${libs.versions.group.id.get()}.lsp.intellij-plugin"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

repositories {
    mavenCentral()
    mavenLocal()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.1")
        pluginVerifier()
        zipSigner()
    }
    implementation(project(":src:compiler:core"))
}

intellijPlatform {
    pluginVerification {
        ides {
            recommended()
        }
    }
    publishing {
        token = System.getenv("JETBRAINS_TOKEN")
        channels = listOf("stable")
    }
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
}

tasks {
    val createOpenAPISourceJar by registering(Jar::class) {
        // Java sources
        from(sourceSets.main.get().java) {
            include("**/community/flock/**/*.java")
        }

        destinationDirectory.set(layout.buildDirectory.dir("libs"))
        archiveClassifier.set("src")
    }

    buildPlugin {
        dependsOn(createOpenAPISourceJar)
        from(createOpenAPISourceJar) { into("scripts") }
    }
}
