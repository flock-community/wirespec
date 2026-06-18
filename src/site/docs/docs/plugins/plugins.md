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
- **share:** A flag to indicate whether shared code should be emitted.
- **strict:** A flag to enable strict mode during compilation.

[Playground compile](http://playground.wirespec.io/compile)

### Convert

The `Convert` operation facilitates integration with other API specification languages by providing an automated way to convert them to Wirespec. It accepts the following inputs:

- **input:** Path to the input file in the original specification language.
- **output:** Path to the output directory where the converted Wirespec file will be placed.
- **format:** The format of the input file (e.g., `OpenAPIV2`, `OpenAPIV3`, `Avro`).

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
- **extension:** The file extension for the output files.
- **split:** A boolean flag indicating whether to split the output into separate files.

## IR extensions

Before generating code, Wirespec lowers your definitions into a language-neutral **intermediate
representation** (IR). An `IrExtension` lets you reshape that IR — for example to inject framework-specific
annotations — without forking an emitter. The transformed IR is then handed to the normal code generator,
so the output stays idiomatic for the target language.

You register extensions with the `irExtensions` parameter of the compile/custom operations. The
Maven and Gradle plugins accept a list of `IrExtension` classes and instantiate them for you, injecting the
`packageName` and `shared` settings into the constructor when the extension needs them.

:::note
Extensions only run when the emitter is an `IrEmitter`. For the built-in language targets this means you
must enable the IR pipeline by setting `ir = true` (Gradle) / `<ir>true</ir>` (Maven). A custom
`emitterClass` that implements `IrEmitter` always honors the registered extensions.
:::

Wirespec ships several IR extensions in its integration modules:

| Extension | Module | Effect |
|---|---|---|
| `KotlinxSerializationExtension` | `kotlinx-serialization` | Adds `@Serializable`/`@SerialName` to generated Kotlin models — see [kotlinx.serialization](../integration/integration-kotlinx-serialization.mdx) |

Extensions whose constructor only needs `packageName`/`shared` (or nothing at all), such as
`KotlinxSerializationExtension`, can be registered directly through `irExtensions`. Extensions
that need other arguments — for instance the target language — are typically wired into a custom
`IrEmitter` instead.

See [Gradle](./plugins-gradle.md) and [Maven](./plugins-maven.md) for the exact configuration syntax.
