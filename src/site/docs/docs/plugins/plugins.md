---
sidebar_position: 1
---

# Plugins

Wirespec supports various plugins for integration into a variety of ecosystems. These plugins are multiplatform, meaning they are written in the corresponding language of the target platform.

## Operations

All plugins support three core operations: Compile, Convert, and Custom.

### Compile

The `Compile` operation transforms Wirespec source code into emitted output files. It accepts the following inputs:

- **input:** Path to the input Wirespec file or directory.
- **output:** Path to the output directory where the generated code will be placed.
- **languages:** A comma-separated list of target languages for code generation (e.g., `Java`, `Kotlin`, `TypeScript`, `Python`, `Wirespec`, `OpenAPIV2`, `OpenAPIV3`).
- **package name:** The package name for the generated code.
- **extensions:** A list of [bundled IR extensions](#bundled-extensions) to apply by name.
- **share:** A flag to indicate whether shared code should be emitted.
- **strict:** A flag to enable strict mode during compilation.

[Playground compile](http://playground.wirespec.io/compile)

### Convert

The `Convert` operation facilitates integration with other API specification languages by providing an automated way to convert them to Wirespec. It accepts the following inputs:

- **input:** Path to the input file in the original specification language.
- **output:** Path to the output directory where the converted Wirespec file will be placed.
- **format:** The format of the input file (e.g., `OpenAPIV2`, `OpenAPIV3`, `Avro`).
- **extensions:** A list of [bundled IR extensions](#bundled-extensions) to apply by name.

[Playground convert](http://playground.wirespec.io/covert)

### Custom

The `Custom` operation combines the functionality of both `Compile` and `Convert` and offers additional options for integrating with custom emitters. It accepts the following inputs:

- **input:** Path to the input file or folder.
- **output:** Path to the output directory.
- **format:** Input format (e.g., `OpenAPIV2`, `OpenAPIV3`, `Avro`).
- **languages:** A comma-separated list of target languages for code generation (e.g., `Java`, `Kotlin`, `TypeScript`, `Python`, `Wirespec`,`OpenAPIV2`, `OpenAPIV3`).
- **package name:** The package name for the generated code.
- **share:** A flag to indicate whether shared code should be emitted.
- **strict:** A flag to enable strict mode during processing.
- **emitterClass:** The fully qualified name of the custom emitter class.
- **extensions:** A list of [bundled IR extensions](#bundled-extensions) to apply by name.
- **extension:** The file extension for the output files.
- **split:** A boolean flag indicating whether to split the output into separate files.

## IR extensions

Before generating code, Wirespec lowers your definitions into a language-neutral **intermediate
representation** (IR). An `IrExtension` lets you reshape that IR — for example to inject framework-specific
annotations — without forking an emitter. The transformed IR is then handed to the normal code generator,
so the output stays idiomatic for the target language.

:::note
Extensions only run when the emitter is an `IrEmitter`. The built-in language targets always emit
through the IR pipeline, so registered extensions take effect out of the box. A custom
`emitterClass` that implements `IrEmitter` also honors the registered extensions.
:::

### Bundled extensions

The extensions that ship with Wirespec's integration modules are bundled with every plugin — the CLI
included — and are enabled by shorthand name; no classpath configuration is needed:

| Name | Targets | Effect |
|---|---|---|
| `Avro` | Java, Kotlin | Appends an Avro schema + converter declaration next to every generated record and enum — see [Avro](../integration/integration-avro.mdx) |
| `Jackson` | Java, Kotlin | Adds Jackson annotations so generated models (de)serialize correctly — see [Jackson](../integration/integration-jackson.mdx) |
| `KotlinxSerialization` | Kotlin | Adds `@Serializable`/`@SerialName` to generated models — see [kotlinx.serialization](../integration/integration-kotlinx-serialization.mdx) |
| `SpringMappingAnnotations` | Java, Kotlin | Adds Spring MVC mapping annotations to every endpoint handler — see [Spring](../integration/integration-spring.mdx) |
| `SpringNativeHints` | Java, Kotlin | Emits a `WirespecNativeHints` file registering models and endpoints for GraalVM native images |
| `KotestDsl` | Kotlin | Generates a typesafe Kotest scenario DSL for endpoints, channels and types |

How to enable them per plugin (names are matched against the table above):

- **CLI**: repeat `-x`/`--extension`, e.g. `wirespec compile -i openapi.ws -l Kotlin -x Jackson -x SpringMappingAnnotations`
- **Gradle**: `irExtensions = listOf(Extension.Jackson, Extension.SpringMappingAnnotations)`
- **Maven**: `<extensions><extension>Jackson</extension><extension>SpringMappingAnnotations</extension></extensions>`

### Custom extensions

The Maven and Gradle plugins additionally accept your own `IrExtension` classes through the
`extensionClasses` parameter and instantiate them for you, injecting the `packageName`, `shared`
and target-language settings into the constructor when the extension needs them. The CLI cannot
load custom classes, so it supports the bundled extensions only.

See [CLI](./plugins-cli.md), [Gradle](./plugins-gradle.md) and [Maven](./plugins-maven.md) for the
exact configuration syntax.
