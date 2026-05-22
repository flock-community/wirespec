plugins {
    id("module.publication")
    id("module.spotless")
    alias(libs.plugins.kotlin.multiplatform)
}

group = "${libs.versions.group.id.get()}.integration"
version = System.getenv(libs.versions.from.env.get()) ?: libs.versions.default.get()

repositories {
    mavenCentral()
    mavenLocal()
}

val codegenClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    codegenClasspath(project(":src:integration:wiremock-codegen"))
}

val generatedWirespecDir = layout.buildDirectory.dir("generated/sources/wirespec")

val generateWirespecTestSources by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Generate Java + Kotlin Wirespec test sources from .ws files in src/jvmTest/resources/wirespec."
    classpath = codegenClasspath
    mainClass.set("community.flock.wirespec.integration.wiremock.codegen.MainKt")
    val inputDir = layout.projectDirectory.dir("src/jvmTest/resources/wirespec")
    val outDir = generatedWirespecDir
    inputs.dir(inputDir).withPropertyName("wirespecSources")
    outputs.dir(outDir).withPropertyName("generatedSources")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                inputDir.asFile.absolutePath,
                outDir.get().asFile.absolutePath,
                "community.flock.wirespec.integration.wiremock",
            )
        },
    )
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
                compileOnly(libs.wiremock)
            }
        }
        jvmTest {
            kotlin.srcDir(generatedWirespecDir.map { it.dir("kotlin") })
            dependencies {
                implementation(project(":src:integration:jackson"))
                implementation(libs.bundles.jackson)
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlin.junit)
                implementation(libs.wiremock)
                runtimeOnly(libs.junit.launcher)
            }
        }
    }
}

// Add the generated Java directory to the jvm test compilation. The KMP plugin doesn't
// expose Java source dirs via the sourceSets DSL, so we wire it on the underlying compile task.
tasks.named<JavaCompile>("compileJvmTestJava") {
    source(generatedWirespecDir.map { it.dir("java") })
}

listOf("compileTestKotlinJvm", "compileJvmTestJava").forEach { name ->
    tasks.named(name) { dependsOn(generateWirespecTestSources) }
}
