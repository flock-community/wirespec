# ClientIrExtension — Design

**Date:** 2026-04-20
**Status:** Draft
**Scope:** Extract the Client feature out of `IrEmitter` into a self-contained `ClientIrExtension`, as the first instance of a general "IR extension" pattern.

## Motivation

`IrEmitter` currently owns orchestration, all per-definition emit methods, and the Client feature (`emitClient`, `emitEndpointClient`, plus shared Wirespec client/server interfaces). Each of the six language emitters (Java, Kotlin, Python, Rust, Scala, TypeScript) overrides both client methods with its own imports, packaging, and language-specific transforms. That makes `IrEmitter` and every language emitter larger and less focused than they need to be.

The goal is to reduce `IrEmitter` complexity by grouping functionality by feature. The Client feature goes first because it already has a clean entry point (two methods called from `emit(ast, logger)`), and because an untracked `TestIrExtension.kt` stub already hints at the intended pattern.

This spec covers only the Client extraction. Subsequent features (e.g. Test, Shared) will follow the same pattern but are out of scope here.

## Architecture

### `IrExtension` base interface

A new marker interface establishes the extension pattern for `IrEmitter`:

```kotlin
package community.flock.wirespec.ir.extensions

interface IrExtension
```

`IrExtension` is intentionally empty — it exists to give all feature-scoped extensions (`ClientIrExtension` now; `TestIrExtension` and others later) a common supertype and a shared home in the `ir.extensions` package. It documents the pattern and lets `IrEmitter` reference "an extension" without coupling to a specific feature.

`ClientIrExtension` extends it:

```kotlin
interface ClientIrExtension : IrExtension {
    fun emitEndpointClient(endpoint: Endpoint): File
    fun emitClient(endpoints: List<Endpoint>, logger: Logger): File
    fun buildClientServerInterfaces(style: AccessorStyle): List<Element>
}
```

Lives in `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/extensions/IrExtension.kt` (separate file from `ClientIrExtension.kt` because it's the general pattern, not a feature).

### Pluggable strategy

`IrEmitter` no longer owns client emit logic. It gains a single abstract property that holds a list of extensions:

```kotlin
interface IrEmitter : Emitter {
    val extensions: List<IrExtension>
    // ...
}
```

Orchestration resolves the extension it needs by type. A small helper keeps call sites tidy:

```kotlin
inline fun <reified T : IrExtension> IrEmitter.extension(): T =
    extensions.filterIsInstance<T>().singleOrNull()
        ?: error("No ${T::class.simpleName} registered on this emitter")
```

Client orchestration then looks like:

```kotlin
val client = extension<ClientIrExtension>()
val mainClient = client.emitClient(allEndpoints, logger)
val clientFiles = endpoints.map { client.emitEndpointClient(it) }
```

Each language emitter registers its extensions once:

```kotlin
override val extensions: List<IrExtension> = listOf(
    KotlinClientIrExtension(packageName, sanitizationConfig, wirespecImport),
    // future: KotlinTestIrExtension(...), etc.
)
```

Trade-off: typing is less precise than a dedicated `val clientExtension: ClientIrExtension` slot — missing or duplicate extensions fail at runtime instead of compile time. The win is that adding a new feature-extension never touches `IrEmitter`'s interface: emitters opt in by appending to the list.

The `emit(ast, logger)` orchestration resolves the extension by type (see "Pluggable strategy" below) and delegates to it. Each language emitter constructs a language-specific `ClientIrExtension` and registers it in its `extensions` list.

### Feature boundary

`ClientIrExtension` owns the **full** Client boundary:

- The two emit methods (language-specific).
- The language-neutral AST→IR conversion (`convertClient`, `convertEndpointClient`, currently in `ir.converter.IrConverter`).
- The shared `buildClientServerInterfaces(AccessorStyle)` helper (currently a free function in `ir.emit.SharedBuilder`).

After the refactor, nothing client-shaped lives outside `ClientIrExtension.kt`.

### File layout

Neutral pieces in the `ir` module:
`src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/extensions/IrExtension.kt` — empty marker interface.
`src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/extensions/ClientIrExtension.kt` — the interface + language-neutral top-level `internal` helpers (`Endpoint.convertEndpointClient()`, `List<Endpoint>.convertClient()`, `buildClientServerInterfaces(style)`).

Per-language concrete classes live next to their respective emitters — one file per language:
- `src/compiler/emitters/kotlin/.../KotlinClientIrExtension.kt`
- `src/compiler/emitters/java/.../JavaClientIrExtension.kt`
- `src/compiler/emitters/python/.../PythonClientIrExtension.kt`
- `src/compiler/emitters/rust/.../RustClientIrExtension.kt`
- `src/compiler/emitters/scala/.../ScalaClientIrExtension.kt`
- `src/compiler/emitters/typescript/.../TypeScriptClientIrExtension.kt`

Rationale: per-language impls call emitter-private helpers (e.g. Java's `transformTypeDescriptors`, Rust's `toRustTypeString`). Those helpers are also used by non-client emit code; promoting them into the `ir` module would be scope creep. Keeping per-language classes in their emitter module preserves the existing module dependency direction (`emitters → ir`, never the reverse).

Each concrete class:
- Takes the state it needs via constructor parameters (e.g. `PackageName`, `SanitizationConfig`, a language-specific `wirespecImport` reference).
- Contains as `private` members the helpers that are only used by client code (e.g. Scala's `addIdentityTypeToCall`, Python's `addSelfReceiverToClientFields` / `snakeCaseClientFunctions` / `flattenEndpointTypeRefs`, Rust's `buildClientParams`).
- Delegates to top-level neutral helpers in `ir.extensions` for AST→IR conversion.

Helpers shared with non-client emitter code (Java's `transformTypeDescriptors`, Rust's `toRustTypeString`, `Endpoint.importReferences`, etc.) stay as `internal` members on the emitter and are accessed from the extension via a back-reference: each extension takes a constructor param that exposes the helpers it needs (either the emitter instance itself, or specific lambdas).

## Interface

```kotlin
package community.flock.wirespec.ir.extensions

interface ClientIrExtension : IrExtension {
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
- Add `val extensions: List<IrExtension>` (abstract).
- Add the inline helper `extension<T>()` for typed lookup.
- Update `emit(ast, logger)` to resolve `extension<ClientIrExtension>()` and call its `emitClient` / `emitEndpointClient` instead of self-dispatched methods.

### Language emitters (all six)

- Remove the `override fun emitEndpointClient(...)` and `override fun emitClient(...)` method bodies.
- Remove private helpers that are now owned by `ClientIrExtension` (e.g. `transformTypeDescriptors` in Java, `addIdentityTypeToCall` in Scala, `addSelfReceiverToClientFields`/`snakeCaseClientFunctions`/`flattenEndpointTypeRefs` in Python).
- Add `override val extensions: List<IrExtension> = listOf(XxxClientIrExtension(...))` wired with the emitter's `packageName`, `sanitizationConfig`, and any language-specific constants.
- Change `sanitizationConfig` from `private val by lazy { ... }` to `internal val` (still lazy; only the visibility changes, and only within the emitter's module).
- In `shared.source` construction, replace `buildClientServerInterfaces(AccessorStyle.PROPERTIES)` with `extension<ClientIrExtension>().buildClientServerInterfaces(AccessorStyle.PROPERTIES)`.

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

`VerifyUtil` integration tests (compile + run across all six languages) are unchanged. They exercise `emitter.emit(ast, logger)`, which continues to work because the orchestration resolves the `ClientIrExtension` from the emitter's extensions list and delegates through it.

## Risks and trade-offs

1. **Per-language files live in the emitter modules.** The neutral `ClientIrExtension.kt` stays in `ir.extensions`, but the six concrete classes live in their respective emitter modules. Single-file-per-feature is preserved at the neutral level; per-language files are one-per-language, co-located with the emitter they extend.
2. **Visibility change.** `sanitizationConfig` moves from `private` to `internal` on each language emitter. Module-local only; no public API change.
3. **Atomic migration.** The `IrEmitter` interface change (removal of `emitClient`/`emitEndpointClient`, addition of abstract `extensions: List<IrExtension>`) forces all six language emitters to migrate in the same commit. No partial state.
4. **Runtime vs. compile-time typing.** Resolving a `ClientIrExtension` from `List<IrExtension>` via `extension<T>()` fails at runtime if the extension is missing or duplicated (the helper throws with a clear message). A typed slot would have caught this at compile time. Accepted in exchange for letting future extensions land without touching `IrEmitter`'s interface.
5. **Shared helpers.** A few utilities (`importReferences`, naming helpers) are used by both client and non-client emitter code. They stay where they are — `ClientIrExtension` imports them. If a helper is used *only* by client code (e.g. Java's `transformTypeDescriptors`), it moves into the extension class.
6. **Scope creep.** Only Client moves in this refactor. `TestIrExtension` (already stubbed) and any future extensions are deferred — but the `ir.extensions` package is explicitly established here so that pattern lands.

## Success criteria

- `IrExtension` marker interface exists at `ir.extensions.IrExtension` and is extended by `ClientIrExtension`.
- `IrEmitter` exposes `val extensions: List<IrExtension>` and the inline `extension<T>()` lookup helper; it no longer declares `emitClient` or `emitEndpointClient`.
- Each language emitter's `extensions` list contains exactly one `ClientIrExtension` instance.
- `IrConverter` no longer declares `convertClient` or `convertEndpointClient`.
- `SharedBuilder` no longer declares `buildClientServerInterfaces` as a free function (or retains a thin delegate if needed during migration — removed by end of refactor).
- All six language emitters expose `override val clientExtension`, and the six client override method bodies are removed.
- `ClientIrExtension.kt` compiles as the single home for the feature.
- Existing `VerifyUtil` integration tests (compile+run across all six languages) still pass unchanged.
- New `ClientIrExtensionTest.kt` passes with neutral + per-language snapshot coverage (two fixture endpoints × six languages × two methods = 24 per-language assertions, plus ~5–10 neutral assertions).
