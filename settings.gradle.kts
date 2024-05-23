pluginManagement {

    val kotlinVersion = "1.9.24"
    val kotestVersion = "5.7.2"

    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("jvm") version kotlinVersion
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
    "src:compiler:lib",
    "src:lsp:jvm:core",
    "src:lsp:jvm:server",
    "src:lsp:jvm:client",
    "src:lsp:intellij-plugin",
    "src:plugin:arguments",
    "src:plugin:cli",
    "src:plugin:maven",
    "src:plugin:npm",
    "src:plugin:gradle",
    "src:converter:openapi",
    "src:integration:jackson",
    "src:integration:wirespec",
    "src:generator",
)
