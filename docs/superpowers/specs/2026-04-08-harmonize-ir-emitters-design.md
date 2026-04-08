# Harmonize IrEmitter Implementations

**Date:** 2026-04-08
**Goal:** Make all 6 IrEmitter implementations (Java, Kotlin, Python, Rust, Scala, TypeScript) structurally consistent by using the same building blocks, without forcing rigid inheritance.

**Constraints:**
- Generated output must be functionally equivalent (not byte-for-byte identical)
- No base class hierarchy — shared utilities as extension functions
- Flexible composition per language, consistent vocabulary across all emitters

## 1. Shared Sanitization

### Problem
`sanitizeNames()` is duplicated across all 6 emitters with minor variations. The core logic (transform field names, parameter names, FieldCall, ConstructorStatement, FunctionCall) is the same.

### Design
Extract a shared `sanitizeNames(config)` extension function on `Element` in the `ir` module:

```kotlin
data class SanitizationConfig(
    val reservedKeywords: Set<String>,
    val escapeKeyword: (String) -> String,       // "_$it" (Java/Python), backtick (Kotlin/Scala), "r#$it" (Rust)
    val fieldNameCase: (Name) -> Name,            // camelCase (Java/Kotlin/Scala/TS), snakeCase (Rust/Python)
    val parameterCase: (Name) -> Name,
    val sanitizeSymbol: (String) -> String,
    val extraStatementTransforms: ((Statement, Transformer) -> Statement)? = null,
)
```

Each emitter defines its config and calls `sanitizeNames(config)`. Language-specific statement transforms (e.g., Kotlin's ConstructorStatement handling, TypeScript's VariableReference/Assignment handling) plug in via `extraStatementTransforms` — a single `(Statement, Transformer) -> Statement` lambda where the emitter handles its special cases in a `when` block and falls through to `stmt.transformChildren(tr)` for the rest.

### Location
`src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/SanitizationConfig.kt`

## 2. Import and Package Wrapping

### Problem
`emit(definition)` is nearly identical across Java, Kotlin, and Scala (compute sub-package, wrap with Package + wirespec import + elements). Python/Rust do similar work for module systems. This is duplicated in each emitter.

### Design
Extract helpers as extension functions:

```kotlin
// For package-based languages (Java, Kotlin, Scala)
fun File.wrapWithPackage(
    packageName: PackageName,
    definition: Definition,
    wirespecImport: Element,
    needsImport: Boolean,
    nameTransform: (Name) -> String = { it.pascalCase() },
): File

// For module-based languages (Python, Rust)
fun File.wrapWithModuleImport(
    packageName: PackageName,
    definition: Definition,
    imports: List<Element>,
    nameTransform: (Name) -> String,
): File
```

Also extract shared source emission helper:

```kotlin
fun NonEmptyList<File>.withSharedSource(
    emitShared: EmitShared,
    sharedFile: () -> File,
): NonEmptyList<File>
```

### Location
`src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/EmitHelpers.kt`

## 3. Raw String Replacement

### Problem
Several emitters bypass the converter+generator pipeline and emit raw code strings, making the codebase inconsistent and harder to maintain.

### Design

**Replace with IR DSL:**
- **Kotlin companion object** (`KotlinIrEmitter:333-354`): Replace raw trimMargin string with IR DSL builders (struct/namespace with function, field, returns), similar to Java's `buildHandlers()`.
- **Scala Client/Server objects** (`ScalaIrEmitter:402-435`): Replace two raw strings with IR DSL namespace/struct builders.

**Isolate into named builder methods (IR DSL cannot express these constructs):**
- **TypeScript api const** (`TypeScriptIrEmitter:201-217`): Extract into `buildApiConst(endpoint): Element`.
- **Rust Api struct + impl blocks** (`RustIrEmitter:814-831`, `786-794`): Extract into `buildApiStruct(endpoint): Element` and `buildHandlerImpl(endpoint): Element`.

**Principle:** Replace raw strings when the IR DSL can express the construct. When it can't, isolate into a well-named `build<Thing>(endpoint): Element` method.

## 4. Consistent Endpoint Emission Pipeline

### Problem
Each emitter composes `emit(endpoint)` in a different order with different method names. Some sanitize before enrichment, meaning enrichment code must handle already-sanitized names.

### Design
All emitters follow this pipeline shape:

```
emit(endpoint) {
    1. Build imports
    2. Convert:   endpoint.convert()
    3. Flatten:   (if needed - Python, Scala, Rust)
    4. Enrich:    inject language-specific constructs
    5. Sanitize:  .sanitizeNames(config)
    6. Wrap:      prepend imports
}
```

Concrete changes per emitter:

| Emitter    | Key change |
|------------|-----------|
| Java       | Move sanitize after injectHandleFunction and transformTypeDescriptors |
| Kotlin     | Replace raw companion object with IR DSL `buildCompanionObject()`, move sanitize after enrichment |
| Scala      | Replace raw Client/Server with IR DSL, move sanitize after flatten+enrich |
| Python     | Reorder: convert -> flatten -> enrich -> sanitize -> wrap |
| TypeScript | Extract `buildApiConst()`, reorder: convert -> transformSwitches -> enrich -> sanitize -> wrap |
| Rust       | Break `rustifyEndpoint()` into named steps, reorder: convert -> flatten -> stripPrefix -> enrich (named steps) -> sanitize -> wrap |

**Key invariant:** Sanitization always happens after enrichment, before import wrapping.

## 5. Shared Source Builder

### Problem
Java, Kotlin, and Scala define structurally identical ClientEdge/ServerEdge/Client/Server interfaces in their `shared` objects, differing only in accessor style (getter methods vs properties).

### Design
Extract a shared builder:

```kotlin
enum class AccessorStyle { GETTER_METHODS, PROPERTIES }

fun buildClientServerInterfaces(style: AccessorStyle): List<Element>
```

Each emitter's `shared` becomes:

```kotlin
override val shared = object : Shared {
    override val source = AstShared(packageString)
        .convert()
        .transform {
            injectImports(languageSpecificImports)
            injectAfterWirespec(buildClientServerInterfaces(style) + languageSpecificExtras)
        }
        .generate()
}
```

Python, TypeScript, and Rust have fundamentally different shared code and stay language-specific, but should follow consistent naming (`buildClientServerInterfaces()` or equivalent).

### Location
`src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/emit/SharedBuilder.kt`

## Files Affected

### New files
- `ir/emit/SanitizationConfig.kt` — shared sanitization config and extension function
- `ir/emit/EmitHelpers.kt` — import/package wrapping helpers
- `ir/emit/SharedBuilder.kt` — shared client/server interface builder

### Modified files
- `emitters/java/JavaIrEmitter.kt` — adopt shared sanitization, package helpers, reorder endpoint pipeline
- `emitters/kotlin/KotlinIrEmitter.kt` — adopt shared sanitization, package helpers, replace raw companion object, reorder endpoint pipeline
- `emitters/scala/ScalaIrEmitter.kt` — adopt shared sanitization, package helpers, replace raw Client/Server objects, reorder endpoint pipeline
- `emitters/python/PythonIrEmitter.kt` — adopt shared sanitization, module helpers, reorder endpoint pipeline
- `emitters/typescript/TypeScriptIrEmitter.kt` — adopt shared sanitization, extract `buildApiConst()`, reorder endpoint pipeline
- `emitters/rust/RustIrEmitter.kt` — adopt shared sanitization, module helpers, break apart `rustifyEndpoint()`, reorder endpoint pipeline

## Verification Strategy

Each emitter has existing tests in `src/compiler/emitters/<lang>/src/commonTest/`. After each change, run the language's test suite to verify functional equivalence:

```
./gradlew :src:compiler:emitters:java:allTests
./gradlew :src:compiler:emitters:kotlin:allTests
./gradlew :src:compiler:emitters:python:allTests
./gradlew :src:compiler:emitters:rust:allTests
./gradlew :src:compiler:emitters:scala:allTests
./gradlew :src:compiler:emitters:typescript:allTests
```