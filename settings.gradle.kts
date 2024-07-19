pluginManagement {

    val kotlinVersion = "2.0.0"

    plugins {
        kotlin("multiplatform") version kotlinVersion
        kotlin("jvm") version kotlinVersion
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
    "src:ide:intellij-plugin",
    "src:plugin:arguments",
    "src:plugin:cli",
    "src:plugin:maven",
    "src:plugin:npm",
    "src:plugin:gradle",
    "src:converter:openapi",
    "src:integration:jackson",
    "src:integration:wirespec",
    "src:tools:generator",
)
