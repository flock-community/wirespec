rootProject.name = "wirespec"

pluginManagement {
    includeBuild("gradle/plugins/publish-sonatype")
    includeBuild("gradle/plugins/spotless")
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

include(
    "src:bom",
    "src:compiler:core",
    "src:compiler:lib",
    "src:ide:intellij-plugin",
    "src:plugin:arguments",
    "src:plugin:cli",
    "src:plugin:maven",
    "src:plugin:npm",
    "src:plugin:gradle",
    "src:converter:common",
    "src:converter:avro",
    "src:converter:openapi",
    "src:integration:avro",
    "src:integration:jackson",
    "src:integration:wirespec",
    "src:integration:spring",
    "src:tools:generator",
)
