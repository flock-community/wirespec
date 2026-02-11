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
    "src:compiler:test",
    "src:compiler:emitters:kotlin",
    "src:compiler:emitters:java",
    "src:compiler:emitters:typescript",
    "src:compiler:emitters:python",
    "src:compiler:emitters:wirespec",
    "src:compiler:test-updater",
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
    "src:language",
)
