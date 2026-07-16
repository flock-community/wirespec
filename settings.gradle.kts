rootProject.name = "wirespec"

pluginManagement {
    includeBuild("gradle/plugins/publish-sonatype")
    includeBuild("gradle/plugins/spotless")
    includeBuild("gradle/plugins/emitter-fixtures")
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
    "src:compiler:emitters:rust",
    "src:compiler:emitters:scala",
    "src:compiler:emitters:wirespec",
    "src:ide:intellij-plugin",
    "src:ide:lsp",
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
    "src:integration:kotest",
    "src:integration:kotlinx-serialization",
    "src:integration:wirespec",
    "src:integration:wiremock",
    "src:integration:spring",
    "src:tools:generator",
    "src:compiler:ir",
    "src:verify",
)
