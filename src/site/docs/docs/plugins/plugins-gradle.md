---
title: Gradle
slug: /plugins/gradle
sidebar_position: 4
---

# Wirespec Gradle Plugin

![Maven Central](https://img.shields.io/maven-central/v/community.flock.wirespec.plugin.maven/wirespec-maven-plugin)

This document describes how to use the Wirespec Gradle plugin to integrate Wirespec compilation into your Gradle build process. The plugin allows you to automatically generate code from your Wirespec definitions during your build.

## Installation

To use the Wirespec Gradle plugin, you need to add it to your `build.gradle.kts` file. Here's how:

1. **Add the Plugin:**

```kts
plugins {
    id("community.flock.wirespec.plugin.gradle") version "{{WIRESPEC_VERSION}}"
}

// The plugin automatically registers a default task named "wirespec"
// You can also register custom tasks as shown below:

tasks.register<CompileWirespecTask>("wirespec-typescript") {
    description = "Compile Wirespec to TypeScript"
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName.set("community.flock.wirespec.generated.typescript")
    languages.set(listOf(Language.TypeScript))
    shared.set(true)
    strict.set(false)
}

tasks.register<ConvertWirespecTask>("wirespec-openapi") {
    description = "Convert JSON to OpenAPISpec"
    input = layout.projectDirectory.file("src/main/openapi/schema.json")
    output = layout.buildDirectory.dir("openapi")
    format = Format.OpenAPIV2
    // Example of using preProcessor to modify the input content before conversion
    preProcessor = { it }
}

// Example of using a custom emitter class
tasks.register<CompileWirespecTask>("wirespec-kotlin") {
    description = "Compile Wirespec to Kotlin"
    group = "Wirespec compile"
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName.set("community.flock.wirespec.generated.kotlin")
    emitterClass.set(KotlinSerializableEmitter::class.java)
    shared = true
    strict = false
}

// Example of a custom emitter class built on the IR pipeline: it extends the standard
// KotlinIrEmitter and reshapes the IR File produced for every definition before generation.
class KotlinSerializableEmitter(packageName: PackageName) : KotlinIrEmitter(packageName, EmitShared(false)) {

    override fun emit(definition: Definition, module: Module, logger: Logger): File =
        super.emit(definition, module, logger).let { file ->
            file.copy(elements = listOf(RawElement("@kotlinx.serialization.Serializable")) + file.elements)
        }
}
```

**Note:** You'll need to add the following imports to your build script:

```
import community.flock.wirespec.plugin.gradle.CompileWirespecTask
import community.flock.wirespec.plugin.gradle.ConvertWirespecTask
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.Format
```

## Task Types

The Wirespec Gradle plugin provides two main task types:

### CompileWirespecTask

This task compiles Wirespec definitions to various target languages.

**Properties:**

- `input`: DirectoryProperty - The input directory containing Wirespec files
- `output`: DirectoryProperty - The output directory for generated code
- `languages`: ListProperty&lt;Language&gt; - List of target languages (Java, Kotlin, TypeScript, Python, Wirespec, OpenAPIV2, OpenAPIV3)
- `packageName`: Property&lt;String&gt; - Package name for generated code
- `emitterClass`: Property&lt;Class&lt;\*&gt;&gt; - Custom emitter class
- `irExtensions`: ListProperty&lt;Extension&gt; - [bundled IR extensions](./plugins.md#bundled-extensions) applied to the intermediate representation before code generation when an emitter is an `IrEmitter`
- `extensionClasses`: ListProperty&lt;Class&lt;\*&gt;&gt; - custom `IrExtension` classes, applied like `irExtensions`
- `shared`: Property&lt;Boolean&gt; - Whether to emit shared code (default: true)
- `strict`: Property&lt;Boolean&gt; - Strict parsing mode (default: false)

### ConvertWirespecTask

This task converts from JSON or Avro to other formats.

**Properties:**

- `input`: RegularFileProperty - The input file (JSON or Avro)
- `output`: DirectoryProperty - The output directory for generated code
- `format`: Property&lt;Format&gt; - The target format (OpenAPIV2, OpenAPIV3, Avro)
- `preProcessor`: Property&lt;(String) -> String&gt; - Function to preprocess the input content before conversion
- `packageName`: Property&lt;String&gt; - Package name for generated code
- `emitterClass`: Property&lt;Class&lt;\*&gt;&gt; - Custom emitter class
- `irExtensions`: ListProperty&lt;Extension&gt; - [bundled IR extensions](./plugins.md#bundled-extensions) applied to the intermediate representation before code generation when an emitter is an `IrEmitter`
- `extensionClasses`: ListProperty&lt;Class&lt;\*&gt;&gt; - custom `IrExtension` classes, applied like `irExtensions`
- `shared`: Property&lt;Boolean&gt; - Whether to emit shared code (default: true)
- `strict`: Property&lt;Boolean&gt; - Strict parsing mode (default: false)

## Applying IR extensions

The [bundled IR extensions](./plugins.md#bundled-extensions) are enabled with the `irExtensions`
property (named this way because Gradle itself reserves `extensions` on every task); they ship with
the plugin, so no extra classpath configuration is needed. The built-in language targets always emit
through the IR pipeline:

```gradle title="build.gradle.kts"
import community.flock.wirespec.plugin.Extension
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.gradle.CompileWirespecTask

tasks.register<CompileWirespecTask>("wirespec-compile") {
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.generated.kotlin"
    languages = listOf(Language.Kotlin)
    shared = false
    irExtensions = listOf(Extension.KotlinxSerialization)
}
```

Custom extensions are registered with the `extensionClasses` property. Put the artifact that
provides the extension on the `buildscript` classpath, then reference the class directly; both
properties can be combined:

```gradle title="build.gradle.kts"
import com.example.wirespec.MyCustomExtension
import community.flock.wirespec.plugin.gradle.CompileWirespecTask
import community.flock.wirespec.plugin.Language

buildscript {
    dependencies {
        classpath("com.example:my-wirespec-extension:1.0.0")
    }
}

tasks.register<CompileWirespecTask>("wirespec-compile") {
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.generated.kotlin"
    languages = listOf(Language.Kotlin)
    shared = false
    extensionClasses = listOf(MyCustomExtension::class.java)
}
```
