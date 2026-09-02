---
sidebar_position: 1
---

# Plugins

Wirespec supports various plugins for integration into a variety of ecosystems. These plugins are multiplatform, meaning they are written in the corresponding language of the target platform.

## Operations

All plugins support two core operations: Compile and Convert.

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

## IR extensions

Before generating code, Wirespec lowers your definitions into a language-neutral **intermediate
representation** (IR). An `IrExtension` lets you reshape that IR — for example to inject framework-specific
annotations, or to add a file per definition — without forking an emitter. The transformed IR is then handed
to the normal code generator, so the output stays idiomatic for the target language.

You register extensions with the `extensionClasses` parameter of the compile and convert operations. The
Maven and Gradle plugins accept a list of `IrExtension` classes and instantiate them for you, injecting the
`packageName` and `shared` settings and the target language into the constructor when the extension needs them.

:::note
The built-in language targets always emit through the IR pipeline, so registered extensions take effect
out of the box. Extensions run in the order they are listed.
:::

Wirespec ships several IR extensions in its integration modules:

| Extension | Module | Effect |
|---|---|---|
| `KotlinxSerializationExtension` | `kotlinx-serialization` | Adds `@Serializable`/`@SerialName` to generated Kotlin models — see [kotlinx.serialization](../integration/integration-kotlinx-serialization.mdx) |

An extension's constructor may declare zero or more parameters of type `PackageName`, `EmitShared`, and
`FileExtension` (the target language of the emitter being extended); the plugins inject all of them. To
write your own extension, see [Architecture › Plugins](../architecture/architecture-plugins.md#ir-extensions)
and the worked example at `examples/maven-spring-custom/`.

See [Gradle](./plugins-gradle.md) and [Maven](./plugins-maven.md) for the exact configuration syntax.
