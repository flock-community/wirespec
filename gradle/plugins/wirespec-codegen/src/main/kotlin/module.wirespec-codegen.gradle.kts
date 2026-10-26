import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// Generates a module's JVM test sources from Wirespec at build time.
//
// Adds a private `codegen` compilation to the jvm target, associated with `main` so the
// module's own IR extensions can be registered, and runs its `<basePackage>.codegen.MainKt`
// (built on `:src:compiler:test`) as `generateWirespecTestSources`. The generated
// `java` and `kotlin` directories are compiled with `jvmTest`. Nothing generated is checked in.
//
// Apply alongside the Kotlin Multiplatform plugin and configure via:
//
//     wirespecCodegen {
//         basePackage = "community.flock.wirespec.integration.jackson"
//     }
//
// The module adds the emitters its Main uses to the compilation:
//
//     kotlin.jvm().compilations.named("codegen") {
//         defaultSourceSet.dependencies { implementation(project(":src:compiler:emitters:java")) }
//     }

val ext = extensions.create<WirespecCodegenExtension>("wirespecCodegen").apply {
    sources.convention(layout.projectDirectory.dir("src/jvmCodegen/resources/wirespec"))
    outputDir.convention(layout.buildDirectory.dir("generated/sources/wirespec"))
}

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    val jvm = the<KotlinMultiplatformExtension>().jvm()
    val codegenCompilation = jvm.compilations.create("codegen") {
        associateWith(jvm.compilations.getByName("main"))
        defaultSourceSet.dependencies {
            implementation(project(":src:compiler:test"))
        }
    }
    the<KotlinMultiplatformExtension>().sourceSets.named("jvmTest") {
        kotlin.srcDir(ext.outputDir.dir("kotlin"))
    }

    val generateWirespecTestSources = tasks.register<JavaExec>("generateWirespecTestSources") {
        group = "build"
        description = "Generate Wirespec test sources with this module's codegen Main."
        classpath = files(codegenCompilation.output.classesDirs) + codegenCompilation.runtimeDependencyFiles
        mainClass.set(ext.basePackage.map { "$it.codegen.MainKt" })
        inputs.dir(ext.sources).withPropertyName("wirespecSources")
        outputs.dir(ext.outputDir).withPropertyName("generatedSources")
        // Capture providers, not the extension, so the configuration cache can store the task.
        val sources = ext.sources
        val outputDir = ext.outputDir
        val basePackage = ext.basePackage
        argumentProviders.add(
            CommandLineArgumentProvider {
                listOf(sources.get().asFile.absolutePath, outputDir.get().asFile.absolutePath, basePackage.get())
            },
        )
        dependsOn(codegenCompilation.compileTaskProvider)
    }

    tasks.named<JavaCompile>("compileJvmTestJava") {
        source(ext.outputDir.dir("java"))
        dependsOn(generateWirespecTestSources)
    }

    tasks.named("compileTestKotlinJvm") {
        dependsOn(generateWirespecTestSources)
    }
}
