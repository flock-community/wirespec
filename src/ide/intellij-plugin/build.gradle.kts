import org.jetbrains.intellij.platform.gradle.TestFrameworkType

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
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    implementation(project(":src:compiler:core"))
    testImplementation(project(":src:compiler:emitters:java"))
    testImplementation(project(":src:compiler:emitters:kotlin"))
    testImplementation(libs.junit4)
    testImplementation(libs.opentest4j)
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
    test {
        // Run the bundled Kotlin plugin in K2 mode, as the IDE does by default
        systemProperty("idea.kotlin.plugin.use.k2", "true")
    }
}

tasks {
    val createOpenAPISourceJar = register<Jar>("createOpenAPISourceJar") {
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
