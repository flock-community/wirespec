---
title: Architecture
sidebar_position: 10
---

# Architecture

The Wirespec compiler turns a `.ws` source file into target-language source code through three stages: **Tokenize**, **Parse**, and **Emit**. This page walks through each stage; deeper material on what happens inside an emitter lives in the linked sub-pages.

## Pipeline

```
Wirespec source (string)
      │
      ▼  Tokenize   (LanguageSpec.tokenize)
List<Token>
      │
      ▼  Parse      (Parser.parse)
AST  (Root → Module[] → Definition[])
      │
      ▼  Emit       (one or more emitters)
List<Emitted>  (filename + content per generated file)
```

## Tokenize

The first stage scans the raw source string into a sequence of typed tokens — keywords, identifiers, punctuation, regex literals, and so on. The entry point is `LanguageSpec.tokenize` (see `src/compiler/core/.../tokenize/Tokenizer.kt`). Whitespace is removed by default; the result is a `NonEmptyList<Token>`. The codebase calls this stage the **Tokenizer**; "Lexer" is the same concept, so either term is fine when reading parser literature alongside this code.

## Parse

The parser consumes the token stream and produces a typed AST. The top-level entry point is `Parser.parse` in `src/compiler/core/.../parse/Parser.kt`. Per-construct parsers handle each Wirespec definition: `TypeParser`, `EndpointParser`, `ChannelParser`, `EnumParser`, and `AnnotationParser`. The output is an `AST` value shaped as `Root → Module[] → Definition[]`, where `Definition` is a sealed hierarchy with six variants: `Type`, `Enum`, `Union`, `Refined`, `Endpoint`, and `Channel`. References inside definitions resolve against a small reference type system (`Custom`, `Primitive`, `Iterable`, `Dict`, `Any`, `Unit`), each of which can be marked nullable.

## Emit

The emit stage takes the parsed AST and produces target-language source code. Each registered emitter consumes the same AST and returns a list of `Emitted` values — a filename and content pair per generated file. Multiple emitters can run on the same compile, producing output for several target languages simultaneously. The whole top-level flow is captured by one line in `Compiler.kt`:

```kotlin
fun CompilationContext.compile(source) = emit(parse(source))
```

The result is a flattened `List<Emitted>` ready to write to disk.

## Two emitter styles

The codebase has two coexisting emitter approaches. Both satisfy the same `Emitter` contract — same input AST, same `List<Emitted>` output. The difference is purely internal:

- **Direct emitters** walk the AST and assemble target-language strings directly. Each language construct is rendered by inspecting the AST node and writing the equivalent target syntax. Example: `PythonEmitter` in `src/compiler/emitters/python/.../PythonEmitter.kt`.
- **IR-based emitters** run an internal **Convert → Transform → Generate** pipeline. The AST is converted into Wirespec's language-neutral IR, per-language transforms reshape the IR for the target's idioms, and a generator walks the transformed IR to produce the source string. Example: `PythonIrEmitter` in the same package. See **[IR Model](./architecture-ir.md)** for the details of this internal pipeline.
