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

val generatedWirespecDir = layout.buildDirectory.dir("generated/sources/wirespec")
val wirespecTestSourcesDir = layout.projectDirectory.dir("src/jvmCodegen/resources/wirespec")

kotlin {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.api.get()))
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion(libs.versions.kotlin.language.get()))
    }
    jvm {
        // Private build-time compilation that hosts the Wirespec emitter Main. Kept out
        // of the published artifact, used only by the generateWirespecTestSources task.
        compilations.create("codegen") {
            defaultSourceSet.dependencies {
                implementation(project(":src:compiler:core"))
                implementation(project(":src:compiler:emitters:java"))
                implementation(project(":src:compiler:emitters:kotlin"))
                implementation(libs.kotlin.stdlib)
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
                compileOnly(libs.bundles.jackson)
                compileOnly(libs.bundles.jackson3)
            }
        }
        jvmTest {
            kotlin.srcDir(generatedWirespecDir.map { it.dir("kotlin") })
            dependencies {
                implementation(libs.bundles.jackson)
                implementation(libs.bundles.jackson3)
            }
        }
    }
}

val codegenCompilation = kotlin.jvm().compilations.named("codegen")

val generateWirespecTestSources by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Generate Java + Kotlin Wirespec test sources from .ws files in src/jvmCodegen/resources/wirespec."
    val compilation = codegenCompilation.get()
    classpath = files(compilation.output.classesDirs) + compilation.runtimeDependencyFiles
    mainClass.set("community.flock.wirespec.integration.jackson.codegen.MainKt")
    val inDir = wirespecTestSourcesDir
    val outDir = generatedWirespecDir
    inputs.dir(inDir).withPropertyName("wirespecSources")
    outputs.dir(outDir).withPropertyName("generatedSources")
    val inputAbsolutePath = inDir.asFile.absolutePath
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                inputAbsolutePath,
                outDir.get().asFile.absolutePath,
                "community.flock.wirespec.integration.jackson",
            )
        },
    )
    dependsOn(compilation.compileTaskProvider)
}

tasks.named<JavaCompile>("compileJvmTestJava") {
    source(generatedWirespecDir.map { it.dir("java") })
    dependsOn(generateWirespecTestSources)
}

tasks.named("compileTestKotlinJvm") {
    dependsOn(generateWirespecTestSources)
}
