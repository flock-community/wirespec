pluginManagement {

    val kotlinVersion = "1.7.0"
    val shadowVersion = "7.1.2"

    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("jvm") version kotlinVersion
        id("com.github.johnrengelman.shadow") version shadowVersion
    }
}

rootProject.name = "wire-spec"

include(
    "compiler:core",
    "compiler:cli",
    "lsp:jvm:core",
    "lsp:jvm:server",
    "lsp:jvm:client",
)
