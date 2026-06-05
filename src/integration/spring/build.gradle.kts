plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.api.get()))
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.language.get()))
    }
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
            }
        }
        // Spring Boot 4 scenario: a Jackson-3-only test classpath that exercises the
        // conditional wiring (WirespecJackson3Configuration) the regular Jackson-2 test
        // suite cannot, because Jackson 2 and 3 share jackson-annotations.
        val mainCompilation = compilations.getByName("main")
        compilations.create("jackson3Test") {
            associateWith(mainCompilation)
            defaultSourceSet.dependencies {
                implementation(project(":src:integration:wirespec"))
                implementation(project(":src:integration:jackson"))
                implementation(libs.bundles.jackson3)
                implementation(libs.spring.boot.web)
                implementation(libs.spring.boot.test)
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.junit)
                runtimeOnly(libs.junit.launcher)
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
                compileOnly(project(":src:compiler:core"))
                compileOnly(project(":src:compiler:emitters:kotlin"))
                compileOnly(project(":src:compiler:emitters:java"))
                api(project(":src:integration:wirespec"))
                implementation(project(":src:integration:jackson"))
                // Both Jackson versions are compileOnly: the consumer's Spring Boot
                // (3 → Jackson 2, 4 → Jackson 3) supplies the one actually used. The
                // Wirespec serializer is selected conditionally at runtime, v3 preferred.
                compileOnly(libs.bundles.jackson2)
                compileOnly(libs.bundles.jackson3)
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlinx.coroutines.reactor)
                implementation(libs.spring.boot.web)
                implementation(libs.spring.webflux)
                runtimeOnly(libs.junit.launcher)
            }
        }
        jvmTest {
            dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:converter:openapi"))
                implementation(project(":src:integration:wirespec"))
                implementation(libs.spring.boot.test)
                implementation(libs.bundles.jackson2)
                implementation(libs.kotlin.junit)
                implementation(libs.kotlinx.io.core)
                implementation(libs.wiremock)
            }
        }
    }
}

// Jackson 3 requires a newer jackson-annotations than Spring Boot 3's BOM pins (2.17.2).
// Override it to the Jackson-3-compatible version on the jackson3Test classpath only.
// eachDependency runs after the dependency-management BOM substitution, so it wins.
configurations
    .matching { it.name.startsWith("jvmJackson3Test") }
    .configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-annotations") {
                useVersion("2.21")
                because("Jackson 3 requires jackson-annotations 2.21; Spring Boot 3's BOM pins it lower")
            }
        }
    }

val jvmJackson3TestCompilation = kotlin.jvm().compilations.named("jackson3Test")

val jvmJackson3Test by tasks.registering(Test::class) {
    description = "Runs Spring integration tests on a Jackson-3-only classpath (Spring Boot 4 scenario)."
    group = "verification"
    val compilation = jvmJackson3TestCompilation.get()
    testClassesDirs = compilation.output.classesDirs
    classpath = compilation.output.allOutputs + compilation.runtimeDependencyFiles
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn(jvmJackson3Test)
}
