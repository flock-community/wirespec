import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

// Generates a module's JVM test sources from Wirespec at build time, by running this build's
// own Wirespec CLI (`:src:plugin:cli`) as `generateWirespecTestSources`. The generated `java`
// and `kotlin` directories are compiled with `jvmTest`. Nothing generated is checked in.
//
// Apply alongside the Kotlin Multiplatform plugin and configure via:
//
//     wirespecCodegen {
//         basePackage = "community.flock.wirespec.integration.spring"
//         languages = listOf("Java", "Kotlin")
//         irExtensions = listOf("SpringMappingAnnotations", "SpringNativeHints")
//         conversions = mapOf("petstore.json" to "OpenAPIV3")
//     }

val ext = extensions.create<WirespecCodegenExtension>("wirespecCodegen").apply {
    sources.convention(layout.projectDirectory.dir("src/jvmTest/resources/wirespec"))
    outputDir.convention(layout.buildDirectory.dir("generated/sources/wirespec"))
}

val wirespecCli = configurations.create("wirespecCli") {
    isCanBeConsumed = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
    }
}

dependencies {
    wirespecCli(project(":src:plugin:cli"))
}

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    the<KotlinMultiplatformExtension>().run {
        jvm()
        sourceSets.named("jvmTest") {
            kotlin.srcDir(ext.outputDir.dir("kotlin"))
        }
    }

    val generateWirespecTestSources = tasks.register<GenerateWirespecSources>("generateWirespecTestSources") {
        group = "build"
        description = "Generate Wirespec test sources with the Wirespec CLI."
        cliClasspath.from(wirespecCli)
        sources = ext.sources
        conversions = ext.conversions
        basePackage = ext.basePackage
        languages = ext.languages
        irExtensions = ext.irExtensions
        outputDir = ext.outputDir
    }

    tasks.named<JavaCompile>("compileJvmTestJava") {
        source(ext.outputDir.dir("java"))
        dependsOn(generateWirespecTestSources)
    }

    tasks.named("compileTestKotlinJvm") {
        dependsOn(generateWirespecTestSources)
    }
}
