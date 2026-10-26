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
- **language:** The target language for code generation (e.g., `Java`, `Kotlin`, `TypeScript`, `Python`, `Wirespec`, `OpenAPIV2`, `OpenAPIV3`). A run generates one language: run the plugin once per language to generate several.
- **package name:** The package name for the generated code.
- **share:** A flag to indicate whether shared code should be emitted.
- **strict:** A flag to enable strict mode during compilation.
- **ignore defaults:** A flag to leave field default values out of the emitted code. Required to compile a spec with
  [defaults](../language/types.mdx#default-values) to a language that does not support them yet. See
  [Default values](#default-values).

[Playground compile](http://playground.wirespec.io/compile)

### Convert

The `Convert` operation facilitates integration with other API specification languages by providing an automated way to convert them to Wirespec. It accepts the following inputs:

- **input:** Path to the input file in the original specification language.
- **output:** Path to the output directory where the converted Wirespec file will be placed.
- **format:** The format of the input file (e.g., `OpenAPIV2`, `OpenAPIV3`, `Avro`, `GraphQL`, `AsyncAPI`).
- **ignore defaults:** A flag to leave the default values that the converter reads out of the emitted code. A
  conversion never fails on defaults, see [Default values](#default-values).

[Playground convert](http://playground.wirespec.io/covert)

## Default values

Wirespec fields can have [default values](../language/types.mdx#default-values). Kotlin, Wirespec, Avro and OpenAPI
can write them, while Java, TypeScript, Python, Rust and Scala cannot yet. How a plugin deals with defaults depends on
the operation and on the target language.

| Situation                                                                | Compile                                  | Convert                                  |
|--------------------------------------------------------------------------|------------------------------------------|------------------------------------------|
| Target supports defaults (Kotlin, Wirespec, Avro, OpenAPI)               | Defaults are generated                   | Defaults are generated                   |
| Target does not support defaults (Java, TypeScript, Python, Rust, Scala) | **Error**, unless ignore defaults is on  | Defaults are left out, no error          |
| Invalid default                                                          | **Error**, also with ignore defaults     | The default is dropped, no error         |
| Ignore defaults is on                                                    | Defaults are left out for every language | Defaults are left out for every language |

### Compile

The defaults in a Wirespec file are written by you, so compile never drops one without telling you.

- **Invalid defaults** are always a compile error, also with ignore defaults. Examples are a value of the wrong type,
  a value outside the field's bounds, an entry the enum does not have, or a default on an endpoint query or header.
  See [Validation](../language/types.mdx#validation).
- **Languages without default support** fail when the spec has defaults. There is one error per file, and it names
  every field with a default:

  ```
  Java does not support default values, but these fields have one: Settings.name, Settings.retries. Remove the defaults,
  or leave them out of the generated code with the ignore defaults option: --ignore-defaults for the CLI, or
  ignoreDefaults in the Gradle and Maven plugins.
  ```

  When there are several errors, the plugin reports them all at once.
- **Ignore defaults** removes the defaults after they are validated and before any code is generated. The generated
  code is then the same as for a spec without defaults.

Every compile generates one language. To generate Kotlin with defaults and Java without them, compile them
separately, for example with two Gradle tasks or two Maven executions. Ignore defaults is only set on the Java one.

### Convert

The defaults of a conversion come from the OpenAPI or Avro spec. You did not write them in Wirespec, so convert never
fails on them.

- The converter reads the `default` of OpenAPI properties and Avro record fields. A default that Wirespec cannot
  represent is dropped: a value of the wrong type, an enum symbol the enum does not have, an array or object, or the
  default of an OpenAPI parameter. See the [OpenAPI](../converters/openapi.md#defaults) and
  [Avro](../converters/avro.md#defaults) converters.
- Languages without default support leave the defaults out, without an error. This is the same output as for a spec
  without defaults.
- Ignore defaults also works for convert, to leave the defaults out of Kotlin, Wirespec, Avro or OpenAPI as well.

### How errors are reported

| Plugin | A default error…                                                                                                                                                                                                                       |
|--------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| CLI    | is printed, and `wirespec` exits with status 1                                                                                                                                                                                         |
| Gradle | fails the task, with the error as the failure message                                                                                                                                                                                  |
| Maven  | fails the build, with the error as the failure message                                                                                                                                                                                 |
| npm    | is the same as the CLI for `wirespec` scripts and `cli()`. The programmatic `parse` result reports invalid defaults, but the AST it returns does not carry defaults yet, so `emit` generates code without them and never fails on them |

## IR extensions

Before generating code, Wirespec lowers your definitions into a language-neutral **intermediate
representation** (IR). An `IrExtension` lets you reshape that IR — for example to inject framework-specific
annotations, or to add a file per definition — without forking an emitter. The transformed IR is then handed
to the normal code generator, so the output stays idiomatic for the target language.

Every plugin bundles the extensions that Wirespec ships, so you use them without adding their integration module to
the plugin:

| Plugin | Option                                                                                                   |
|--------|----------------------------------------------------------------------------------------------------------|
| CLI    | `-e`, `--extension`, once per extension: `-e SpringMappingAnnotations -e Jackson`                        |
| Gradle | `extensionClasses = listOf(SpringMappingAnnotationsExtension::class.java, JacksonExtension::class.java)` |
| Maven  | `<extensionClasses><extensionClass>SpringMappingAnnotations</extensionClass></extensionClasses>`         |
| npm    | the `extensions` argument of `emit`: `[Extensions.SPRING_MAPPING_ANNOTATIONS, Extensions.JACKSON]`       |

The [playground](https://playground.wirespec.io) offers them too, next to the output language.

The name of a bundled extension is its class name without `Extension`: `Jackson` selects `JacksonExtension`. In Maven
that name replaces the fully qualified class name; in Gradle you reference the class, which ships with the plugin.

| Name                       | Languages           | Effect                                                                                                                                 |
|----------------------------|---------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `Avro`                     | Java, Kotlin        | Adds an Avro schema and converter next to every model — see [Avro](../integration/integration-avro.mdx)                                |
| `Jackson`                  | Java, Kotlin        | Adds Jackson annotations to every model — see [Jackson](../integration/integration-jackson.mdx)                                       |
| `JvmBuilder`               | Java, Kotlin, Scala | Adds an immutable `Builder` with `builder()` and `toBuilder()` to every model — see [Builders](../integration/integration-builders.mdx) |
| `KotestDsl`                | Kotlin              | Adds a Kotest scenario DSL for every model, endpoint, channel and rpc                                                                  |
| `KotlinxSerialization`     | Kotlin              | Adds `@Serializable`/`@SerialName` to every model — see [kotlinx.serialization](../integration/integration-kotlinx-serialization.mdx) |
| `SpringMappingAnnotations` | Java, Kotlin        | Adds Spring MVC mapping annotations to every endpoint handler — see [Spring](../integration/integration-spring.mdx)                   |
| `SpringNativeHints`        | Java, Kotlin        | Adds a `WirespecNativeHints` class that registers the models and endpoints for GraalVM native images                                  |

The names are case-insensitive in the CLI. An extension applies to the languages it supports and leaves the others
untouched, so one run can emit Kotlin with `KotlinxSerialization` next to TypeScript without it.

Your own extensions go in the same `extensionClasses` parameter of the Maven and Gradle plugins, by class. The plugins
instantiate them for you, injecting the `packageName` and `shared` settings and the target language into the
constructor when the extension needs them. Your own extensions apply to every language.

:::note
The built-in language targets always emit through the IR pipeline, so registered extensions take effect
out of the box. Extensions run in the order they are listed.
:::

An extension's constructor may declare zero or more parameters of type `PackageName`, `EmitShared`, and
`FileExtension` (the target language of the emitter being extended); the plugins inject all of them. To
write your own extension, see [Architecture › Plugins](../architecture/architecture-plugins.md#ir-extensions)
and the worked example at `examples/maven-spring-custom/`.

See [Gradle](./plugins-gradle.md) and [Maven](./plugins-maven.md) for the exact configuration syntax.
