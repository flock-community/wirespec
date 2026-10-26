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
    basePackage = "community.flock.wirespec.integration.jackson"
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
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                // The JacksonExtension only reshapes the language-neutral IR with
                // fully-qualified raw annotations, so it needs the compiler/IR API
                // at compile time but no Jackson runtime — that comes from the
                // consumer's build.
                compileOnly(project(":src:compiler:core"))
                compileOnly(project(":src:compiler:emitters:kotlin"))
                compileOnly(project(":src:compiler:emitters:java"))
                compileOnly(project(":src:integration:wirespec"))
            }
        }
        commonTest {
            dependencies {
                implementation(project(":src:integration:wirespec"))
                implementation(libs.bundles.kotlin.test)
                implementation(libs.bundles.kotest)
            }
        }
        jvmMain {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:emitters:kotlin"))
                implementation(project(":src:compiler:emitters:java"))
                compileOnly(libs.bundles.jackson2)
                compileOnly(libs.bundles.jackson3)
            }
        }
        jvmTest {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:emitters:kotlin"))
                implementation(project(":src:compiler:emitters:java"))
                implementation(libs.bundles.jackson2)
                implementation(libs.bundles.jackson3)
            }
        }
    }
}
