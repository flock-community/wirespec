pluginManagement {

    val kotlinVersion = "1.7.10"
    val shadowVersion = "7.1.2"

    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("jvm") version kotlinVersion
        id("com.github.johnrengelman.shadow") version shadowVersion
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}



rootProject.name = "wire-spec"

include(
    "compiler:core",
    "compiler:cli",
    "compiler:lib",
    "lsp:jvm:core",
    "lsp:node:server",
    "lsp:jvm:client",
    "lsp:intellij-plugin",
)
