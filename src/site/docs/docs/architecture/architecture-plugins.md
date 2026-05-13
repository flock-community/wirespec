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

## Custom emitters

Wirespec accepts a user-supplied emitter alongside the built-in languages. **Maven and Gradle support this**; the CLI and NPM plugins do not.

A custom emitter implements `Emitter` from `compiler/core/.../emit/Emitter.kt`:

```kotlin
interface Emitter : HasExtension {
    fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted>
}
```

In practice most custom emitters extend the `LanguageEmitter` abstract base (same package) for boilerplate around comment styles, file extensions, and reusable formatting helpers. The constructor may declare zero or more parameters of type `PackageName` and/or `EmitShared` — the plugin injects both from the build configuration. Any other constructor parameter is rejected at load time.

The plugins load the class from the build classpath via reflection. Maven takes the fully-qualified name as a string:

```xml
<emitterClass>com.example.MyCustomEmitter</emitterClass>
```

Gradle takes the class object directly:

```kotlin
emitterClass.set(MyCustomEmitter::class.java)
```

The custom emitter runs *in addition to* any languages selected for the same task — its `Emitted` output is concatenated with the built-ins'. Wirespec's own first-party integrations (`AvroJavaEmitter`, `AvroKotlinEmitter`, `SpringJavaEmitter`) are themselves wired in through this mechanism, so the [Integration](../integration/integration.mdx) pages double as worked examples. A minimal end-to-end project lives at `examples/maven-spring-custom/`.

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
