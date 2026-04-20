# ClientIrExtension — Design

**Date:** 2026-04-20
**Status:** Draft
**Scope:** Extract the Client feature out of `IrEmitter` into a self-contained `ClientIrExtension`, as the first instance of a general "IR extension" pattern.

## Motivation

`IrEmitter` currently owns orchestration, all per-definition emit methods, and the Client feature (`emitClient`, `emitEndpointClient`, plus shared Wirespec client/server interfaces). Each of the six language emitters (Java, Kotlin, Python, Rust, Scala, TypeScript) overrides both client methods with its own imports, packaging, and language-specific transforms. That makes `IrEmitter` and every language emitter larger and less focused than they need to be.

The goal is to reduce `IrEmitter` complexity by grouping functionality by feature. The Client feature goes first because it already has a clean entry point (two methods called from `emit(ast, logger)`), and because an untracked `TestIrExtension.kt` stub already hints at the intended pattern.

This spec covers only the Client extraction. Subsequent features (e.g. Test, Shared) will follow the same pattern but are out of scope here.

## Architecture

### Pluggable strategy

`IrEmitter` no longer owns client emit logic. It gains a single abstract property:

```kotlin
interface IrEmitter : Emitter {
    val clientExtension: ClientIrExtension
    // ...
}
```

The `emit(ast, logger)` orchestration delegates:

```kotlin
val mainClient = clientExtension.emitClient(allEndpoints, logger)
val clientFiles = endpoints.map { clientExtension.emitEndpointClient(it) }
```

Each language emitter constructs a language-specific `ClientIrExtension` and exposes it:

```kotlin
override val clientExtension: ClientIrExtension = KotlinClientIrExtension(
    packageName = packageName,
    sanitizationConfig = sanitizationConfig,
    wirespecImport = wirespecImport,
)
```

### Feature boundary

`ClientIrExtension` owns the **full** Client boundary:

- The two emit methods (language-specific).
- The language-neutral AST→IR conversion (`convertClient`, `convertEndpointClient`, currently in `ir.converter.IrConverter`).
- The shared `buildClientServerInterfaces(AccessorStyle)` helper (currently a free function in `ir.emit.SharedBuilder`).

After the refactor, nothing client-shaped lives outside `ClientIrExtension.kt`.

### File layout

Single file: `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/extensions/ClientIrExtension.kt`.

Contents, in order:

1. Package + imports.
2. The `ClientIrExtension` interface.
3. Language-neutral top-level `internal` helpers: `Endpoint.convertEndpointClient()`, `List<Endpoint>.convertClient()`, and the neutral `buildClientServerInterfaces(style)` implementation.
4. Six concrete classes: `KotlinClientIrExtension`, `JavaClientIrExtension`, `PythonClientIrExtension`, `RustClientIrExtension`, `ScalaClientIrExtension`, `TypeScriptClientIrExtension`.

Each concrete class:
- Takes the state it needs via constructor parameters (e.g. `PackageName`, `SanitizationConfig`, a language-specific `wirespecImport` string).
- Contains as `private` members the helpers that are only used by client code (e.g. Java's `transformTypeDescriptors`, Scala's `addIdentityTypeToCall`, Python's `addSelfReceiverToClientFields` / `snakeCaseClientFunctions` / `flattenEndpointTypeRefs`).
- Delegates to top-level neutral helpers for AST→IR conversion.

Helpers shared with non-client emitter code (`Endpoint.importReferences`, `String.firstToUpper`, package-name utilities) stay where they are. The extension calls them the same way the emitter does.

Expected size: ~700–900 lines. This is intentional: a single file is the agreed source of truth for the Client feature.

## Interface

```kotlin
package community.flock.wirespec.ir.extensions

interface ClientIrExtension {
    fun emitEndpointClient(endpoint: Endpoint): File
    fun emitClient(endpoints: List<Endpoint>, logger: Logger): File
    fun buildClientServerInterfaces(style: AccessorStyle): List<Element>
}
```

Each method is implemented by every per-language class. The neutral `convertEndpointClient`/`convertClient` helpers are NOT on the interface — they are top-level functions in the same file, callable from the concrete classes (which typically call them first and then layer on language-specific packaging).

## Wiring changes

### `IrEmitter`

- Remove `fun emitEndpointClient(endpoint: Endpoint): File = endpoint.convertEndpointClient()`.
- Remove `fun emitClient(endpoints: List<Endpoint>, logger: Logger): File { ... endpoints.convertClient() }`.
- Add `val clientExtension: ClientIrExtension` (abstract).
- Update `emit(ast, logger)` to call `clientExtension.emitClient(...)` and `clientExtension.emitEndpointClient(...)` instead of self-dispatched methods.

### Language emitters (all six)

- Remove the `override fun emitEndpointClient(...)` and `override fun emitClient(...)` method bodies.
- Remove private helpers that are now owned by `ClientIrExtension` (e.g. `transformTypeDescriptors` in Java, `addIdentityTypeToCall` in Scala, `addSelfReceiverToClientFields`/`snakeCaseClientFunctions`/`flattenEndpointTypeRefs` in Python).
- Add `override val clientExtension: ClientIrExtension = XxxClientIrExtension(...)` wired with the emitter's `packageName`, `sanitizationConfig`, and any language-specific constants.
- Change `sanitizationConfig` from `private val by lazy { ... }` to `internal val` (still lazy; only the visibility changes, and only within the emitter's module).
- In `shared.source` construction, replace `buildClientServerInterfaces(AccessorStyle.PROPERTIES)` with `clientExtension.buildClientServerInterfaces(AccessorStyle.PROPERTIES)`.

### `IrConverter.kt`

- Remove `fun EndpointWirespec.convertEndpointClient(): File` and `fun List<EndpointWirespec>.convertClient(): File`. Move the bodies into `ClientIrExtension.kt` as `internal` top-level helpers.

### `SharedBuilder.kt`

- Remove the free `buildClientServerInterfaces(style)` function. Move the body into `ClientIrExtension.kt` as a neutral helper, called from each concrete class's `buildClientServerInterfaces` override (or made the default impl if the same for all languages — review during implementation; today all callers pass one of two `AccessorStyle` values and get the same result modulo that style, so the implementation is likely shared).

## Tests

Single new test file: `src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/extensions/ClientIrExtensionTest.kt`.

### Neutral section

Tests exercise `convertEndpointClient`, `convertClient`, and the neutral `buildClientServerInterfaces` helper against fixture `Endpoint` values. Assertions target the `File` IR tree — struct/interface names, function signatures, response union membership, field counts. No language source strings here.

### Per-language sections

For each of Kotlin, Java, Python, Rust, Scala, TypeScript:

- One test calling `<lang>ClientIrExtension(...).emitEndpointClient(fixture)` and asserting the generated source string (via `generateKotlin()` / `generateJava()` / etc.) equals a golden multi-line string defined inline in the test.
- One test for `emitClient` using a list of fixture endpoints.

### Fixtures

Fixtures are constructed directly in the test file — no `.ws` parsing. Two endpoints cover the surface:

1. `GET /ping → 200 Unit` — minimal path, no params, no body.
2. `POST /users/{id}` with a JSON body, one path param, one query param, and a `201 UserCreated` response — exercises imports, handler wrapping, request construction.

That coverage is enough to catch regressions in language-specific packaging (imports, subpackage, `thenApply` wrapper in Java, `addSelfReceiverToClientFields` in Python, `addIdentityTypeToCall` in Scala).

### Existing tests

`VerifyUtil` integration tests (compile + run across all six languages) are unchanged. They exercise `emitter.emit(ast, logger)`, which continues to work because the orchestration simply delegates through `clientExtension`.

## Risks and trade-offs

1. **File size.** `ClientIrExtension.kt` will be ~700–900 lines. Editing Java client code puts the cursor next to Python client code. Accepted deliberately in exchange for a single feature home.
2. **Visibility change.** `sanitizationConfig` moves from `private` to `internal` on each language emitter. Module-local only; no public API change.
3. **Atomic migration.** The `IrEmitter` interface change (removal of `emitClient`/`emitEndpointClient`, addition of abstract `clientExtension`) forces all six language emitters to migrate in the same commit. No partial state.
4. **Shared helpers.** A few utilities (`importReferences`, naming helpers) are used by both client and non-client emitter code. They stay where they are — `ClientIrExtension` imports them. If a helper is used *only* by client code (e.g. Java's `transformTypeDescriptors`), it moves into the extension class.
5. **Scope creep.** Only Client moves in this refactor. `TestIrExtension` (already stubbed) and any future extensions are deferred — but the `ir.extensions` package is explicitly established here so that pattern lands.

## Success criteria

- `IrEmitter` no longer declares `emitClient` or `emitEndpointClient`.
- `IrConverter` no longer declares `convertClient` or `convertEndpointClient`.
- `SharedBuilder` no longer declares `buildClientServerInterfaces` as a free function (or retains a thin delegate if needed during migration — removed by end of refactor).
- All six language emitters expose `override val clientExtension`, and the six client override method bodies are removed.
- `ClientIrExtension.kt` compiles as the single home for the feature.
- Existing `VerifyUtil` integration tests (compile+run across all six languages) still pass unchanged.
- New `ClientIrExtensionTest.kt` passes with neutral + per-language snapshot coverage (two fixture endpoints × six languages × two methods = 24 per-language assertions, plus ~5–10 neutral assertions).
