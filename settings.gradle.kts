pluginManagement {

    val kotlinVersion = "1.9.10"
    val shadowVersion = "7.1.2"
    val kotestVersion = "5.7.2"

    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("jvm") version kotlinVersion
        id("com.github.johnrengelman.shadow") version shadowVersion
        id("io.kotest.multiplatform") version kotestVersion
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
    "src:compiler:core",
    "src:compiler:cli",
    "src:compiler:lib",
    "src:lsp:jvm:core",
    "src:lsp:jvm:server",
    "src:lsp:jvm:client",
    "src:lsp:intellij-plugin",
    "src:plugin:maven",
    "src:plugin:gradle",
    "src:converter:openapi",
)
