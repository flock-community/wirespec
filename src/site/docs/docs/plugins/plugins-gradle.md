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
    language.set(Language.TypeScript)
    shared.set(true)
    strict.set(false)
}

tasks.register<ConvertWirespecTask>("wirespec-openapi") {
    description = "Convert JSON to OpenAPISpec"
    input = layout.projectDirectory.file("src/main/openapi/schema.json")
    output = layout.buildDirectory.dir("openapi")
    format = Format.OpenAPIV2
    language = Language.Wirespec
    // Example of using preProcessor to modify the input content before conversion
    preProcessor = { it }
}

// Example of applying a custom extension
tasks.register<CompileWirespecTask>("wirespec-kotlin") {
    description = "Compile Wirespec to Kotlin"
    group = "Wirespec compile"
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName.set("community.flock.wirespec.generated.kotlin")
    language.set(Language.Kotlin)
    extensionClasses.set(listOf(SerializableExtension::class.java))
    shared = true
    strict = false
}

// Example of an IR extension: it receives the language-neutral IR of every generated file
// and annotates each top-level struct before the generator turns the tree into Kotlin.
class SerializableExtension : IrExtension {

    override fun extend(ir: IR, ast: AST): IR = ir.map { element ->
        if (element is File) element.copy(elements = element.elements.flatMap { it.annotated() }) else element
    }

    private fun Element.annotated(): List<Element> =
        if (this is Struct) listOf(RawElement("@kotlinx.serialization.Serializable"), this) else listOf(this)
}
```

**Note:** You'll need to add the following imports to your build script:

```
import community.flock.wirespec.plugin.gradle.CompileWirespecTask
import community.flock.wirespec.plugin.gradle.ConvertWirespecTask
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.extension.IrExtension
```

## Task Types

The Wirespec Gradle plugin provides two main task types, `CompileWirespecTask` and `ConvertWirespecTask`, and
`ClasspathWirespecTask` to take their input from the classpath.

### CompileWirespecTask

This task compiles Wirespec definitions to one target language. Register a task per language to generate several.

**Properties:**

- `input`: DirectoryProperty - The input directory containing Wirespec files
- `output`: DirectoryProperty - The output directory for generated code
- `language`: Property&lt;Language&gt; - The target language (Java, Kotlin, TypeScript, Python, Php, Rust, Scala, Wirespec, OpenAPIV2, OpenAPIV3, Avro)
- `packageName`: Property&lt;String&gt; - Package name for generated code
- `extensionClasses`: ListProperty&lt;Class&lt;\*&gt;&gt; - `IrExtension` classes applied to the intermediate representation before code generation when an emitter is an `IrEmitter`. The classes of the [extensions bundled with the plugin](./plugins.md#ir-extensions) ship with it and apply to the languages they support
- `shared`: Property&lt;Boolean&gt; - Whether to emit shared code (default: true)
- `strict`: Property&lt;Boolean&gt; - Strict parsing mode (default: false)
- `ignoreDefaults`: Property&lt;Boolean&gt; - Leave field default values out of the emitted code (default: false)
- `sync`: Property&lt;Boolean&gt; - Emit endpoint handlers, channels and rpc services as synchronous functions, see [Synchronous code](./plugins.md#synchronous-code) (default: false)

### ConvertWirespecTask

This task converts from JSON or Avro to other formats.

**Properties:**

- `input`: RegularFileProperty - The input file (JSON or Avro)
- `output`: DirectoryProperty - The output directory for generated code
- `format`: Property&lt;Format&gt; - The target format (OpenAPIV2, OpenAPIV3, Avro, GraphQL, AsyncAPI)
- `language`: Property&lt;Language&gt; - The target language, see `CompileWirespecTask`
- `preProcessor`: Property&lt;(String) -> String&gt; - Function to preprocess the input content before conversion
- `packageName`: Property&lt;String&gt; - Package name for generated code
- `extensionClasses`: ListProperty&lt;Class&lt;\*&gt;&gt; - `IrExtension` classes applied to the intermediate representation before code generation when an emitter is an `IrEmitter`. The classes of the [extensions bundled with the plugin](./plugins.md#ir-extensions) ship with it and apply to the languages they support
- `shared`: Property&lt;Boolean&gt; - Whether to emit shared code (default: true)
- `strict`: Property&lt;Boolean&gt; - Strict parsing mode (default: false)
- `ignoreDefaults`: Property&lt;Boolean&gt; - Leave field default values out of the emitted code (default: false)
- `sync`: Property&lt;Boolean&gt; - Emit endpoint handlers, channels and rpc services as synchronous functions, see [Synchronous code](./plugins.md#synchronous-code) (default: false)

### ClasspathWirespecTask

This task copies files from the classpath to a directory, to use as the input of a `CompileWirespecTask` or a
`ConvertWirespecTask`, see [Wirespec files from the classpath](#wirespec-files-from-the-classpath).

**Properties:**

- `classpathInput`: Property&lt;String&gt; - The file or folder on the classpath
- `classpath`: ConfigurableFileCollection - The jars and directories in which `classpathInput` is looked up
- `output`: DirectoryProperty - The directory to copy the files to. The task replaces its contents
- `outputFile`: Provider&lt;RegularFile&gt; - The copied file, when `classpathInput` is one file

## Default values

A `CompileWirespecTask` fails when the Wirespec files have [default values](../language/types.mdx#default-values) and
its `language` does not support them, such as Java. The task failure names the fields that have a default. Set
`ignoreDefaults` to generate the code without them.

Every task generates one language, so the defaults stay where they are supported when only the tasks of the other
languages set `ignoreDefaults`:

```kotlin
tasks.register<CompileWirespecTask>("wirespec-kotlin") {
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated/kotlin")
    language.set(Language.Kotlin)
}

tasks.register<CompileWirespecTask>("wirespec-java") {
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated/java")
    language.set(Language.Java)
    // Java does not support defaults yet: leave them out instead of failing the task.
    ignoreDefaults.set(true)
}
```

A `ConvertWirespecTask` never fails on defaults: languages without default support leave them out. An invalid default
in a Wirespec file fails the task, also with `ignoreDefaults`. See [Default values](./plugins.md#default-values) for
the details.

## Applying IR extensions

[IR extensions](./plugins.md#ir-extensions) are registered with the `extensionClasses` property. The classes of the
extensions that Wirespec ships are bundled with the plugin, so you reference them without adding a dependency. Put an
extension of your own on the `buildscript` classpath, or declare it in the build script as in the
[installation](#installation) example. The built-in language targets always emit through the IR pipeline:

```gradle title="build.gradle.kts"
import community.flock.wirespec.integration.kotlinxserialization.extension.KotlinxSerializationExtension
import community.flock.wirespec.plugin.gradle.CompileWirespecTask
import community.flock.wirespec.plugin.Language

tasks.register<CompileWirespecTask>("wirespec-compile") {
    input = layout.projectDirectory.dir("src/main/wirespec")
    output = layout.buildDirectory.dir("generated")
    packageName = "community.flock.wirespec.generated.kotlin"
    language = Language.Kotlin
    shared = false
    extensionClasses = listOf(KotlinxSerializationExtension::class.java)
}
```

## Wirespec files from the classpath

To compile Wirespec files from a jar, for example a specification that the team owning the API publishes as an
artifact, register a `ClasspathWirespecTask` and use its result as the `input` of a `CompileWirespecTask`. The task looks
up its `classpathInput` in the jars and directories of its `classpath`, and copies what it finds to its `output`
directory. Like in the [Maven plugin](./plugins-maven.md#wirespec-files-from-the-classpath), the `classpathInput` is
one file or a folder:

- A path to a file, such as `wirespec/petstore.ws`, copies that file.
- Any other path is a folder: the task copies every `.ws` file in it and in its subfolders, across all the jars and
  directories of the `classpath`. Write the folder with slashes, `com/example/api`, or as a package name,
  `com.example.api`. The files all land at the top of the `output` directory, where a `CompileWirespecTask` reads them,
  so the task fails when two of them have the same name.

```kotlin title="build.gradle.kts"
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.gradle.CompileWirespecTask
import community.flock.wirespec.plugin.gradle.ClasspathWirespecTask

val wirespecSpecs = configurations.create("wirespecSpecs")

dependencies {
    wirespecSpecs("com.example:api-specs:1.0.0")
}

val classpathWirespec = tasks.register<ClasspathWirespecTask>("classpathWirespec") {
    classpathInput = "com/example/api"
    classpath.from(wirespecSpecs)
    output = layout.buildDirectory.dir("wirespec-specs")
}

tasks.register<CompileWirespecTask>("wirespec-api") {
    input = classpathWirespec.flatMap { it.output }
    output = layout.buildDirectory.dir("generated")
    packageName = "com.example.api"
    language = Language.Kotlin
}
```

A `ConvertWirespecTask` takes one file: set the `classpathInput` to that file and use the `outputFile` as `input`:

```kotlin title="build.gradle.kts"
import community.flock.wirespec.plugin.Format
import community.flock.wirespec.plugin.Language
import community.flock.wirespec.plugin.gradle.ConvertWirespecTask
import community.flock.wirespec.plugin.gradle.ClasspathWirespecTask

val classpathOpenApi = tasks.register<ClasspathWirespecTask>("classpathOpenApi") {
    classpathInput = "openapi/petstore.json"
    classpath.from(wirespecSpecs)
    output = layout.buildDirectory.dir("openapi-specs")
}

tasks.register<ConvertWirespecTask>("wirespec-openapi") {
    input = classpathOpenApi.flatMap { it.outputFile }
    output = layout.buildDirectory.dir("generated")
    format = Format.OpenAPIV3
    language = Language.Kotlin
    preProcessor = { it }
}
```

Gradle runs the `ClasspathWirespecTask` first, because the `input` comes from its result. To look in the dependencies of
your project instead, as the Maven plugin does, use its compile classpath:
`classpath.from(configurations.compileClasspath)`.

The task fails when the `classpathInput` is not on its `classpath`. Gradle tracks the contents of the `classpath`, so
the task runs again when a jar changes. Give every `ClasspathWirespecTask` an `output` directory of its own: the task
replaces its contents.
