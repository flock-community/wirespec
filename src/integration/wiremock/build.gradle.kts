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

val wirespecCli by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    wirespecCli(project(mapOf("path" to ":src:plugin:cli", "configuration" to "jvmRuntimeElements")))
}

val generatedWirespecDir = layout.buildDirectory.dir("generated/sources/wirespec")
val wirespecTestSourcesDir = layout.projectDirectory.dir("src/jvmTest/resources/wirespec")

fun TaskContainer.registerWirespecGen(name: String, language: String, packageName: String, outputSubdir: String) = register<JavaExec>(name) {
    group = "build"
    description = "Generate $language Wirespec test sources from .ws files in src/jvmTest/resources/wirespec."
    classpath = wirespecCli
    mainClass.set("community.flock.wirespec.plugin.cli.MainKt")
    val inDir = wirespecTestSourcesDir
    val outDir = generatedWirespecDir.map { it.dir(outputSubdir) }
    inputs.dir(inDir).withPropertyName("wirespecSources")
    outputs.dir(outDir).withPropertyName("generatedSources")
    val inputAbsolutePath = inDir.asFile.absolutePath
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                "compile",
                "-i", inputAbsolutePath,
                "-l", language,
                "-p", packageName,
                "-o", outDir.get().asFile.absolutePath,
            )
        },
    )
}

val generateWirespecJavaTestSources by tasks.registerWirespecGen(
    name = "generateWirespecJavaTestSources",
    language = "Java",
    packageName = "community.flock.wirespec.integration.wiremock.java.generated",
    outputSubdir = "java",
)

val generateWirespecKotlinTestSources by tasks.registerWirespecGen(
    name = "generateWirespecKotlinTestSources",
    language = "Kotlin",
    packageName = "community.flock.wirespec.integration.wiremock.kotlin.generated",
    outputSubdir = "kotlin",
)

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
                implementation(project(":src:integration:jackson"))
                implementation(libs.bundles.jackson)
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

tasks.named<JavaCompile>("compileJvmTestJava") {
    source(generatedWirespecDir.map { it.dir("java") })
    dependsOn(generateWirespecJavaTestSources)
}

tasks.named("compileTestKotlinJvm") {
    dependsOn(generateWirespecKotlinTestSources)
}
