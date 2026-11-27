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
    basePackage = "community.flock.wirespec.integration.jsonrpc"
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
                // The extension only adds IR; the compiler comes from the Wirespec plugin that loads it. Nothing of
                // JSON-RPC reaches the generated code but the code itself.
                compileOnly(project(":src:compiler:core"))
            }
        }
        jvmTest {
            // Kotlin tests drive the generated Java client and server too, so the compiler must see the Java sources.
            kotlin.srcDir(wirespecCodegen.outputDir.dir("java"))
            kotlin.srcDir(wirespecCodegen.outputDir.dir("sync"))
            kotlin.srcDir(wirespecCodegen.outputDir.dir("javaspec"))
            kotlin.srcDir(wirespecCodegen.outputDir.dir("kotlinspec"))
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:emitters:java"))
                implementation(project(":src:compiler:emitters:kotlin"))
                implementation(project(":src:compiler:emitters:scala"))
                implementation(project(":src:compiler:emitters:typescript"))
                implementation(project(":src:compiler:emitters:python"))
                implementation(project(":src:compiler:emitters:rust"))
                implementation(project(":src:integration:wirespec"))
                implementation(project(":src:integration:jackson"))
                implementation(project(":src:integration:rust-serde"))
                implementation(libs.bundles.jackson2)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlin.junit)
                implementation(libs.bundles.kotest)
                runtimeOnly(libs.junit.launcher)
            }
        }
    }
}

tasks.named<JavaCompile>("compileJvmTestJava") {
    source(wirespecCodegen.outputDir.dir("javaspec"))
}
