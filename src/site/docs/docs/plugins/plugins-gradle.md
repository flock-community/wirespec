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
    format.set(Format.OpenAPIV2)
}

// Example of using a custom emitter class
tasks.register<CompileWirespecTask>("wirespec-kotlin") {
    description = "Compile Wirespec to Kotlin"
    group = "Wirespec compile"
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName.set("community.flock.wirespec.generated.kotlin")
    emitterClass.set(KotlinSerializableEmitter::class.java)
    shared.set(true)
    strict.set(false)
}

// Example of a custom emitter class
class KotlinSerializableEmitter : KotlinEmitter("community.flock.wirespec.generated.kotlin", noLogger) {

    override fun emit(type: Type, ast: AST): String = """
        |@kotlinx.serialization.Serializable
        |${super.emit(type, ast)}
    """.trimMargin()

    override fun emit(refined: Refined): String = """
        |@kotlinx.serialization.Serializable
        |${super.emit(refined)}
    """.trimMargin()
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
- `languages`: ListProperty&lt;Language&gt; - List of target languages (Java, Kotlin, Scala, TypeScript, Python, Wirespec, OpenAPIV2, OpenAPIV3)
- `packageName`: Property&lt;String&gt; - Package name for generated code
- `emitterClass`: Property&lt;Class&lt;*&gt;&gt; - Custom emitter class
- `shared`: Property&lt;Boolean&gt; - Whether to emit shared code (default: true)
- `strict`: Property&lt;Boolean&gt; - Strict parsing mode (default: false)

### ConvertWirespecTask

This task converts from JSON or Avro to other formats.

**Properties:**
- `input`: RegularFileProperty - The input file (JSON or Avro)
- `output`: DirectoryProperty - The output directory for generated code
- `format`: Property&lt;Format&gt; - The target format (OpenAPIV2, OpenAPIV3, Avro)
- `packageName`: Property&lt;String&gt; - Package name for generated code
- `emitterClass`: Property&lt;Class&lt;*&gt;&gt; - Custom emitter class
- `shared`: Property&lt;Boolean&gt; - Whether to emit shared code (default: true)
- `strict`: Property&lt;Boolean&gt; - Strict parsing mode (default: false)
