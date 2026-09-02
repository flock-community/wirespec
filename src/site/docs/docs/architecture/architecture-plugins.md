---
title: Plugins
sidebar_position: 3
slug: /architecture/plugins
---

# Plugins

Wirespec ships as a **multiplatform compiler** with thin plugin layers that adapt it to different ecosystems and runtimes. The same Tokenize → Parse → Emit pipeline (see [Architecture](./architecture.md)) runs everywhere; plugins just supply input, collect output, and expose configuration in the conventions of their host environment.

## Multiplatform reach

The compiler core (`src/compiler/`) and the shared plugin contract (`src/plugin/arguments/`) are written in Kotlin Multiplatform with three runtime targets:

- **JVM** — packaged as JARs; consumed by Maven, Gradle, and the JVM CLI distribution.
- **Native** — packaged as standalone executables; the CLI ships native builds for macOS, Linux, and Windows.
- **JS** — packaged as an npm module; consumed by the NPM plugin and the [playground](https://playground.wirespec.io).

Because the contract is shared, the JVM, Native, and JS CLI binaries all dispatch through the same `WirespecCli` definition.

## Available plugins

| Plugin | Runtime targets | Use it from |
|---|---|---|
| **CLI** | JVM, Native, JS (Node) | shell, scripts, CI — see [CLI docs](../plugins/plugins-cli.md) |
| **Maven** | JVM | `pom.xml` — see [Maven docs](../plugins/plugins-maven.md) |
| **Gradle** | JVM | Gradle build script — see [Gradle docs](../plugins/plugins-gradle.md) |
| **NPM** | JS (Node) | npm project — see [NPM docs](../plugins/plugins-npm.md) |

Source: `src/plugin/{cli,maven,gradle,npm}`.

## Operations

Every plugin exposes the same two core operations. The shape of the inputs differs only in how they are declared in the host environment.

- **Compile** — Wirespec source → target-language source code. Default for every plugin.
- **Convert** — OpenAPI v2/v3 or Avro JSON → Wirespec source. Used to bootstrap a Wirespec contract from an existing spec.

For the per-option reference and worked invocations, see the [Plugins](../plugins/plugins.md) menu.

## IR extensions

The built-in emitters are customised with **IR extensions** rather than replaced. Every language target lowers the AST into a language-neutral intermediate representation (IR) before generating code; an `IrExtension` receives that complete IR together with the parsed AST and returns the IR that is handed to the code generator. **Maven and Gradle support this**; the CLI and NPM plugins do not.

```kotlin
fun interface IrExtension {
    fun extend(ir: IR, ast: AST): IR
}
```

An extension can add files, drop files, or reshape the elements of existing ones — annotate a struct, append a method, wrap a namespace. Because it operates on the IR rather than on generated text, one extension can serve several target languages and the generator still renders idiomatic code. Extensions are written in Kotlin: the IR is an Arrow `NonEmptyList`, a Kotlin value class that Java code cannot implement an interface against.

Wirespec's own first-party integrations are wired in this way. The Avro integration's `AvroExtension`, for example, appends an `<Type>Avro` schema/converter class for every model when registered on the Java or Kotlin IR emitter (see the [Integration](../integration/integration.mdx) pages, with a worked example at `examples/maven-spring-avro/`).

The plugins load extension classes from the build classpath via reflection and instantiate them for every emitter of the task. The constructor may declare zero or more parameters of type `PackageName`, `EmitShared`, and/or `FileExtension` (the target language of the emitter being extended) — the plugin injects them from the build configuration. Any other constructor parameter is rejected at load time. Maven takes the fully-qualified names as strings:

```xml
<extensionClasses>
    <extensionClass>com.example.MyExtension</extensionClass>
</extensionClasses>
```

Gradle takes the class objects directly:

```kotlin
extensionClasses.set(listOf(MyExtension::class.java))
```

Extensions run in the order they are listed, after all built-in files (models, endpoints, clients, shared code) have been produced and before any code is generated. A minimal end-to-end project lives at `examples/maven-spring-custom/`.

## Shared contract

The `arguments` module is the seam between the compiler and the plugins. Every plugin builds a `WirespecArguments` value (input sources, emitters, writer, error handler, package name, logger, plus the `shared` / `strict` / `ir` flags) and calls the top-level `compile(args)` or `convert(args)` function:

```
plugin (CLI / Maven / Gradle / NPM)
        │  builds
        ▼
WirespecArguments   ──►   compile(args)  /  convert(args)
                                  │
                                  ▼
                       Tokenize → Parse → Emit
                                  │
                                  ▼
                            List<Emitted>
```

Everything platform-specific (Mojo annotations, Gradle property types, Clikt options, npm CLI parsing) sits *above* this contract; everything language- and AST-related sits *below* it. Adding a new plugin — say, an sbt plugin — means writing a new top half against this same contract.
