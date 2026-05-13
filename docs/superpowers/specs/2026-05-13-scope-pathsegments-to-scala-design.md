# Scope `pathSegments` to Scala-only output

**Date:** 2026-05-13
**Status:** Approved for planning

## Goal

`pathSegments` (and the supporting `PathSegment` / `Literal` / `Param` types) should appear only in Scala-generated output. Java, Kotlin, TypeScript, Python, and Rust generated code drops it entirely. The Scala emitter takes ownership of the field.

## Background

`pathSegments` is currently emitted by every language emitter. The shared IR helper `IrConverter.convertClientServer()` declares it as a field on the `Client` and `Server` interfaces and also declares the `PathSegment` sealed type plus `Literal` / `Param` structs. Each language emitter then renders a per-endpoint implementation of `pathSegments` in its native syntax.

Of the six language emitters, only Scala genuinely needs this information. The rest can be simplified by removing it.

## Architecture overview

Two layers are involved per language:

1. **Shared runtime types** — the `PathSegment` sealed type, the `Literal` / `Param` data carriers, and the `pathSegments` field on `Client` / `Server` interfaces. These are emitted into each language's `Wirespec` runtime file.
2. **Per-endpoint emission** — each endpoint emits its own `pathSegments = [...]` value implementing the runtime contract.

| Emitter | Shared types source | Per-endpoint source |
|---|---|---|
| Java | `IrConverter.convertClientServer()` | `JavaIrTransformer.kt` (`getPathSegments` fn) |
| Kotlin | `IrConverter.convertClientServer()` | `KotlinIrTransformer.kt` (companion-object line) |
| Scala | `IrConverter.convertClientServer()` | `ScalaIrTransformer.kt` (Client/Server `object`s) |
| TypeScript | inline raw template in `TypeScriptIrEmitter.kt` | `TypeScriptIrTransformer.kt` |
| Python | inline `pathSegmentStructs` list in `PythonIrEmitter.kt` | `PythonIrTransformer.kt` |
| Rust | inline raw template in `RustIrEmitter.kt` (`PathSegment` enum + `path_segments()` on `Server` trait) | `RustIrTransformer.kt` |

## Design

Decision: **Scala owns the field.** Strip `pathSegments` from the shared IR helper; Scala re-adds what it needs in its own emitter. Other languages drop both their runtime declaration and their per-endpoint emission.

### 1. Shrink `IrConverter.convertClientServer()`

File: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/IrConverter.kt`

Remove from the returned `List<Element>`:

- `` `interface`("PathSegment", isSealed = true) ``
- `struct("Literal") { … }`
- `struct("Param") { … }`
- `field("pathSegments", Type.Array(Type.Custom("PathSegment")))` inside both the `Client` and `Server` interface builders.

After the change, `convertClientServer()` returns only the `ServerEdge`, `ClientEdge`, `Client`, and `Server` interfaces; the last two carry only `pathTemplate` and `method` fields plus their `client` / `server` functions.

### 2. Re-add Scala-specific pieces in `ScalaIrEmitter.kt`

File: `src/compiler/emitters/scala/src/commonMain/kotlin/community/flock/wirespec/emitters/scala/ScalaIrEmitter.kt`

In `emitShared()`, augment the result of `packageName.convertClientServer()` so the Scala `Wirespec` namespace contains:

- The three types (`PathSegment` sealed interface, `Literal` struct, `Param` struct) — same shape as the originals removed from `IrConverter.kt`.
- A `pathSegments: Array<Custom("PathSegment")>` field on each of `Client` and `Server`.

Implementation: a small helper alongside the existing transforms (suggested home: `ScalaIrTransformer.kt`) that either (a) returns the three extra elements to inject before the `Client` interface, plus a `transform` that adds the field to `Client` and `Server`, or (b) returns a complete Scala-specific `List<Element>` to use in place of the shared list. Pick whichever yields the smaller diff at implementation time; both are functionally equivalent.

`ScalaIrTransformer.kt`'s `appendClientServerObjects` is unchanged — it already emits the per-endpoint `override val pathSegments: List[Wirespec.PathSegment] = List(...)`.

### 3. Strip per-endpoint emission from non-Scala transformers

For each, the change is purely local — remove the `pathSegmentsCode` setup and the line that renders it. `pathTemplate` and `method` stay.

- `src/compiler/emitters/kotlin/.../KotlinIrTransformer.kt`: drop `pathSegmentsCode` (lines ~34–39) and the `override val pathSegments…` line (~44).
- `src/compiler/emitters/java/.../JavaIrTransformer.kt`: drop `pathSegmentsCode` setup and the `function("getPathSegments", isOverride = true) { … }` block.
- `src/compiler/emitters/typescript/.../TypeScriptIrTransformer.kt`: drop `pathSegmentsCode` and the `pathSegments: [...]` field in the per-endpoint object literal.
- `src/compiler/emitters/python/.../PythonIrTransformer.kt`: simplify `splitEndpointStructsToModuleLevel` so it no longer takes or emits `pathSegmentsCode`. Update its one call site in `PythonIrEmitter.kt`.
- `src/compiler/emitters/rust/.../RustIrTransformer.kt`: drop `pathSegmentsCode` and the `fn path_segments(&self) -> Vec<PathSegment> { vec![...] }` line.

### 4. Strip shared-runtime emission for the inline templates

- `TypeScriptIrEmitter.kt` (`emitShared()`): from the raw template, remove the `Literal`, `Param`, and `PathSegment` `export type` lines, and remove `pathSegments: PathSegment[]` from the `Api<…>` type.
- `PythonIrEmitter.kt`: remove the `pathSegmentStructs` property and the `injectAfter { namespace: Namespace -> … }` block that injects it inside `emitShared()`.
- `RustIrEmitter.kt`: remove the `pathSegment` `RawElement`, remove `fn path_segments(&self) -> Vec<PathSegment>;` from the `Server` trait raw template, and remove the local element from the returned list.

### 5. Snapshot test updates

Each affected emitter has commonTest snapshot tests asserting on full generated output. Update only the lines impacted:

- `src/compiler/emitters/java/src/commonTest/.../JavaIrEmitterTest.kt`
- `src/compiler/emitters/kotlin/src/commonTest/.../KotlinIrEmitterTest.kt`
- `src/compiler/emitters/typescript/src/commonTest/.../TypeScriptIrEmitterTest.kt`
- `src/compiler/emitters/python/src/commonTest/.../PythonIrEmitterTest.kt`
- `src/compiler/emitters/rust/src/commonTest/.../RustIrEmitterTest.kt`

`ScalaIrEmitterTest.kt` should remain unchanged — its expected output already contains `pathSegments`. If it doesn't compile after the IR change, that signals a regression in step 2.

## Out of scope

- No changes to parser AST (`Endpoint.Segment.Literal` / `Param`).
- No changes to Scala runtime semantics or per-endpoint Scala output.
- No new integration tests; existing snapshot tests in each emitter module cover the surface.

## Verification

Per-language:

```
./gradlew :src:compiler:emitters:java:allTests
./gradlew :src:compiler:emitters:kotlin:allTests
./gradlew :src:compiler:emitters:typescript:allTests
./gradlew :src:compiler:emitters:python:allTests
./gradlew :src:compiler:emitters:rust:allTests
./gradlew :src:compiler:emitters:scala:allTests
```

For each non-Scala emitter, confirm the diff in expected snapshots removes only `pathSegments` / `path_segments` / `getPathSegments` lines (and supporting `PathSegment` declarations in the shared `Wirespec` file). Scala test output must not change.

Final cross-check: `./gradlew check` from the repo root.

## Risks

- **Downstream consumers of `Wirespec.PathSegment` in generated code.** If any example project, generated client, or external user depends on `pathSegments` being present in Java/Kotlin/TS/Python/Rust output, removing it is a breaking change. A repo-wide grep before merge will surface any in-tree consumers.
- **Misaligned shared/per-endpoint emission.** If only the runtime types are dropped but a transformer still emits `pathSegments: …`, the generated code won't compile. The snapshot tests catch this.
