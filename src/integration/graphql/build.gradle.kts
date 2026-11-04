plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
    id("module.wirespec-codegen")
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

wirespecCodegen {
    basePackage = "community.flock.wirespec.integration.graphql"
}

kotlin {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.api.get()))
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.language.get()))
    }
    jvm {
        compilations.named("codegen") {
            defaultSourceSet.dependencies {
                implementation(project(":src:compiler:emitters:java"))
                implementation(project(":src:compiler:emitters:kotlin"))
            }
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                // The extension only adds IR; the compiler comes from the Wirespec plugin that loads it.
                // The GraphQL converter writes the query documents at build time, so nothing GraphQL
                // reaches the generated code.
                compileOnly(project(":src:compiler:core"))
                implementation(project(":src:converter:graphql"))
            }
        }
        jvmTest {
            // Kotlin tests drive the generated Java clients too, so the compiler must see the Java sources.
            kotlin.srcDir(wirespecCodegen.outputDir.dir("java"))
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:emitters:java"))
                implementation(project(":src:compiler:emitters:kotlin"))
                implementation(project(":src:integration:wirespec"))
                implementation(project(":src:integration:jackson"))
                implementation(libs.bundles.jackson2)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlin.junit)
                runtimeOnly(libs.junit.launcher)
            }
        }
    }
}
