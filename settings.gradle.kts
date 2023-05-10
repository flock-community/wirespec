pluginManagement {

    val kotlinVersion = "1.8.21"
    val shadowVersion = "7.1.2"

    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("jvm") version kotlinVersion
        id("com.github.johnrengelman.shadow") version shadowVersion
    }

    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "wirespec"

include(
    "compiler:core",
    "compiler:cli",
    "compiler:lib",
    "lsp:jvm:core",
    "lsp:jvm:server",
    "lsp:jvm:client",
    "lsp:intellij-plugin",
    "plugin:maven",
    "plugin:gradle",
)
