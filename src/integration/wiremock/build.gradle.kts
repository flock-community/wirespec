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
    basePackage = "community.flock.wirespec.integration.wiremock"
    sources = layout.projectDirectory.dir("src/jvmTest/resources/wirespec")
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
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.bundles.kotest)
                implementation(project(":src:integration:wirespec"))
                implementation(project(":src:compiler:test"))
                implementation(project(":src:compiler:core"))
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                api(project(":src:integration:wirespec"))
                implementation(project(":src:integration:jackson"))
                implementation(libs.bundles.jackson2)
                compileOnly(libs.wiremock)
            }
        }
        jvmTest {
            dependencies {
                implementation(project(":src:integration:jackson"))
                implementation(libs.bundles.jackson2)
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlin.junit)
                implementation(libs.wiremock)
                runtimeOnly(libs.junit.launcher)
            }
        }
    }
}
