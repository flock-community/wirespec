# Generator Emitter — Combined Design

**Date:** 2026-04-23
**Status:** Design approved

## Context

The current `src/tools/generator/Generator.kt` produces JSON test data at runtime by walking the parser AST. This design replaces that approach with **generated source code** — for every Wirespec model definition (`Type`, `Enum`, `Union`, `Refined`) the compiler emits a type-safe generator function that delegates leaf-value production to a user-supplied pure callback.

Because the feature is built on the existing IR DSL, all six target languages (Java, Kotlin, TypeScript, Python, Scala, Rust) get generator code "for free" through the shared IR → Transform → CodeGenerator pipeline.

## Goal

Emit a per-model generator file in every target language that:
- lives in a `generator/` subpackage alongside the existing `model/`,
- is fully dependency-free (consistent with project policy),
- delegates every leaf value to a user-provided `Wirespec.Generator` callback, and
- threads a `path: List<String>` through every call so the callback can produce context-aware values.

## Shared Runtime Additions

Two additions to `SharedWirespec.convert()` in `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/IrConverter.kt`, alongside the existing Wirespec runtime types.

### `GeneratorField<T>` — sealed hierarchy (flat siblings)

```text
Wirespec
├── GeneratorField<T>                        (sealed interface)
├── GeneratorFieldString(regex)                       : GeneratorField<String>
├── GeneratorFieldInteger(min, max)                   : GeneratorField<Long>
├── GeneratorFieldNumber(min, max)                    : GeneratorField<Double>
├── GeneratorFieldBoolean                             : GeneratorField<Boolean>
├── GeneratorFieldBytes                               : GeneratorField<ByteArray>
├── GeneratorFieldEnum(values)                        : GeneratorField<String>
├── GeneratorFieldUnion(variants)                     : GeneratorField<String>
├── GeneratorFieldArray(inner: GeneratorField<*>)     : GeneratorField<Int>
├── GeneratorFieldNullable(inner: GeneratorField<*>)  : GeneratorField<Boolean>
├── GeneratorFieldDict(key: GeneratorField<*>,
│             value: GeneratorField<*>)      : GeneratorField<Int>
└── Generator                                (interface)
```

Field constraints map directly from the parser AST's refinement bounds:
- `GeneratorFieldString.regex` — nullable. Populated from `Primitive.Type.String` constraint (regex body with delimiters stripped), or from `Refined`'s regex bound.
- `GeneratorFieldInteger.min / .max` — nullable `Long`. Populated from `Primitive.Type.Integer` constraint, or from `Refined`'s integer bound.
- `GeneratorFieldNumber.min / .max` — nullable `Double`. Populated from `Primitive.Type.Number` constraint, or from `Refined`'s number bound.
- `GeneratorFieldBoolean`, `GeneratorFieldBytes` — no fields.
- `GeneratorFieldEnum.values` — `List<String>`, entries of the enum.
- `GeneratorFieldUnion.variants` — `List<String>`, names of union members.
- `GeneratorFieldArray.inner: GeneratorField<*>?` — descriptor of the element type. **Populated** when the element is a primitive/refined/enum/union; `null` when it is a custom type (the generator will recurse into the sub-generator; the callback cannot descriptor-inspect custom-type internals). Callback returns element **count** (`Int`).
- `GeneratorFieldNullable.inner: GeneratorField<*>?` — descriptor of the wrapped (non-nullable) type. Same populated/`null` convention as `GeneratorFieldArray.inner`. Callback returns **is-null?** (`Boolean`); when `true`, the generator short-circuits to `null` without invoking the inner.
- `GeneratorFieldDict.key: GeneratorField<*>?, GeneratorFieldDict.value: GeneratorField<*>?` — descriptors of the key and value types. Same populated/`null` convention as `GeneratorFieldArray.inner`. Callback returns **entry count** (`Int`).

This "count/flag" semantics for `Array`/`Nullable`/`Dict` keeps the callback to a pure leaf-decision mapping; the generator owns all structural iteration.

### `Generator` — functional interface

```kotlin
interface Generator {
    fun <T> generate(path: List<String>, type: GeneratorType, field: GeneratorField<T>): T
}
```

Parameters:
- `path: List<String>` — segments accumulated as the generator recurses, e.g. `["Person", "addresses", "0", "street"]`.
- `type: GeneratorType` — a marker identifying the Wirespec definition that immediately owns this field. `GeneratorType` is a sealed hierarchy generated per Wirespec module (see "`GeneratorType` — per-module marker hierarchy" below). For a field inside `Address`, `type` is `GeneratorType.Address`; for a field inside `Person`, `type` is `GeneratorType.Person`. For a `Refined` wrapper's inner value, `type` is the refined marker. For an `Enum`'s label, `type` is the enum marker. For a `Union`'s variant pick, `type` is the union marker; after dispatching into a chosen variant, `type` becomes that variant's marker. Because `GeneratorType` is sealed, the callback can pattern-match exhaustively in every language that supports it.
- `field: GeneratorField<T>` — describes what value is needed, with constraints. The generic `T` is inferred; no casts required in callback bodies.

The callback is named `generator` at the call site, but the **parameter name** inside generated functions is `callback` to avoid visually overloading the word "generator" in function bodies. (The parameter name is emitted by `GeneratorConverter` and is purely cosmetic.)

### `GeneratorType` — per-module marker hierarchy

For each Wirespec module, emit an additional file that declares a sealed hierarchy enumerating every `Type`, `Enum`, `Union`, and `Refined` definition in the module (the same set that receives a generator). Channels and Endpoints are excluded.

**File:** `…/generated/generator/GeneratorType.{ext}` — sibling of the `XxxGenerator` files.

**Shape per language:**

| Language | Rendering |
|---|---|
| Kotlin | `sealed interface GeneratorType { object Address : GeneratorType; object Person : GeneratorType; object UUID : GeneratorType; object Color : GeneratorType; object Shape : GeneratorType; object Circle : GeneratorType; object Square : GeneratorType; object Triangle : GeneratorType }` |
| Java | `public sealed interface GeneratorType permits Address, Person, UUID, Color, Shape, Circle, Square, Triangle { record Address() implements GeneratorType {} … }` |
| Scala | `sealed trait GeneratorType; object GeneratorType { case object Address extends GeneratorType; case object Person extends GeneratorType; … }` |
| Rust | `pub enum GeneratorType { Address, Person, UUID, Color, Shape, Circle, Square, Triangle }` |
| TypeScript | `export type GeneratorType = "Address" \| "Person" \| "UUID" \| "Color" \| "Shape" \| "Circle" \| "Square" \| "Triangle"` (string-literal union for exhaustive `switch` with `never` fall-through) |
| Python | `class GeneratorType(str, Enum): Address = "Address"; Person = "Person"; …` (typed `str` enum — supports `match`/`case` exhaustively in 3.10+) |

**Exhaustiveness:**
- Kotlin/Java/Scala/Rust — sealed hierarchies or enums; compilers catch missing cases at build time.
- TypeScript — string-literal union with `switch (type) { … default: const _: never = type }` idiom.
- Python — `match type: case GeneratorType.Address: …` with `case _:` fallback; exhaustive in `py >= 3.10` via `assert_never`.

Callbacks can be written with full compile-time coverage of every definition in the module:

```kotlin
val callback = object : Wirespec.Generator {
    override fun <T> generate(path, type, field): T = when (type) {
        is GeneratorType.Address -> …
        is GeneratorType.Person -> …
        is GeneratorType.UUID -> …
        is GeneratorType.Color -> …
        is GeneratorType.Shape -> …
        is GeneratorType.Circle -> …
        is GeneratorType.Square -> …
        is GeneratorType.Triangle -> …
    }
}
```

(Name collision note: the markers live in their own sealed namespace `GeneratorType.Xxx`, so they don't clash with the model classes `Address`, `Person`, etc.)

## `GeneratorConverter.kt` — IR → generator-function IR

**New file:** `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt`.

Four public entry points, dispatched from the language-emitter hook (Section "Emitter Integration"):

```kotlin
fun TypeWirespec.convertToGenerator(): File
fun EnumWirespec.convertToGenerator(): File
fun UnionWirespec.convertToGenerator(): File
fun RefinedWirespec.convertToGenerator(): File
// Endpoints and Channels produce no generator — the hook returns null.
```

Each produces an IR `File` whose single `Function` is named `generate` and lives inside a singleton namespace named `{TypeName}Generator`.

### Generated shape per definition (illustrated in Kotlin; the IR is language-neutral)

#### Type (struct with fields)

Given:
```wirespec
type Address { street: String, number: Integer, postalCode: UUID }
```

Emitted:
```kotlin
object AddressGenerator {
    fun generate(path: List<String>, callback: Wirespec.Generator): Address = Address(
        street = callback.generate(path + "street", GeneratorType.Address,
            Wirespec.GeneratorFieldString(regex = null)),
        number = callback.generate(path + "number", GeneratorType.Address,
            Wirespec.GeneratorFieldInteger(min = null, max = null)),
        postalCode = UUIDGenerator.generate(path + "postalCode", callback),
    )
}
```

In the pseudocode below, `<typeMarker>` stands for the `GeneratorType` marker of the enclosing definition (e.g. `GeneratorType.Address` when generating a field of `Address`).

Field handling by `Reference`:
- **Primitive** → `callback.generate(path + fieldName, <typeMarker>, <Primitive-to-XxxField>)`.
- **Custom** → `{FieldTypeName}Generator.generate(path + fieldName, callback)` — recursive composition.
- **Iterable** →
  ```kotlin
  val count = callback.generate(path + fieldName, <typeMarker>,
      Wirespec.GeneratorFieldArray(inner = <innerDesc>))
  val values = (0 until count).map { i ->
      <recurse-on-inner>(path + fieldName + i.toString(), callback)
  }
  ```
- **Dict** →
  ```kotlin
  val count = callback.generate(path + fieldName, <typeMarker>,
      Wirespec.GeneratorFieldDict(key = <keyDesc>, value = <valueDesc>))
  val entries = (0 until count).associate { i ->
      <keyGen>(path + fieldName + "key$i", callback) to
      <valueGen>(path + fieldName + "value$i", callback)
  }
  ```
- **Nullable reference** (any of the above with `isNullable = true`) → wrap the value production:
  ```kotlin
  val isNull = callback.generate(path + fieldName, <typeMarker>,
      Wirespec.GeneratorFieldNullable(inner = <nonNullableDesc>))
  val value = if (isNull) null else <non-nullable-production>
  ```

Descriptor filling for `<innerDesc>` / `<keyDesc>` / `<valueDesc>` / `<nonNullableDesc>`:
- **Primitive** (including `Refined` wrappers and enum/union references treated as primitives): emit the matching `Wirespec.XxxField(...)` constructor.
- **Custom**: emit `null`. The callback inspects only what it can meaningfully reason about; the generator owns the recursion into the custom sub-generator afterwards.
- **Nested Iterable / Dict / Nullable inside another Iterable / Dict / Nullable**: recursively build the matching descriptor with the same rules.

#### Refined (constrained primitive)

Given:
```wirespec
type UUID /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/g
```

Emitted:
```kotlin
object UUIDGenerator {
    fun generate(path: List<String>, callback: Wirespec.Generator): UUID = UUID(
        value = callback.generate(path + "value", GeneratorType.UUID,
            Wirespec.GeneratorFieldString(regex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")),
    )
}
```

Integer/Number refined types emit `GeneratorFieldInteger(min, max)` / `GeneratorFieldNumber(min, max)` with bounds from the refinement constraint.

#### Enum

```kotlin
object ColorGenerator {
    fun generate(path: List<String>, callback: Wirespec.Generator): Color = Color(
        label = callback.generate(path + "value", GeneratorType.Color,
            Wirespec.GeneratorFieldEnum(values = listOf("RED", "GREEN", "BLUE"))),
    )
}
```

Assumes each language's enum emission produces a `label: String` constructor parameter — this is already the convention across current Wirespec emitters.

#### Union

```kotlin
object ShapeGenerator {
    fun generate(path: List<String>, callback: Wirespec.Generator): Shape {
        val variant = callback.generate(path + "variant", GeneratorType.Shape,
            Wirespec.GeneratorFieldUnion(variants = listOf("Circle", "Square", "Triangle")))
        return when (variant) {
            "Circle" -> CircleGenerator.generate(path + "Circle", callback)
            "Square" -> SquareGenerator.generate(path + "Square", callback)
            "Triangle" -> TriangleGenerator.generate(path + "Triangle", callback)
            else -> error("Unknown variant: $variant")
        }
    }
}
```

## Emitter Integration

### `IrEmitter` interface (`src/compiler/ir/.../emit/IrEmitter.kt`)

Add two hooks — one per-definition (for the `XxxGenerator` files) and one per-module (for the `GeneratorType` marker hierarchy):

```kotlin
fun emitGenerator(definition: Definition, module: Module): File? = null
fun emitGeneratorType(module: Module): File? = null
```

Extend `emit(module, logger)` so that after assembling `definitionFiles + clientFiles`, it collects per-definition generator files and the single per-module `GeneratorType` file:
```kotlin
val generatorFiles = module.statements.toList()
    .filterIsInstance<Model>()
    .mapNotNull { model ->
        logger.info("Emitting Generator for ${model::class.simpleName} ${model.identifier.value}")
        emitGenerator(model, module)
    }
val generatorTypeFile = listOfNotNull(emitGeneratorType(module).also { if (it != null) logger.info("Emitting GeneratorType for module") })
return definitionFiles + clientFiles + generatorFiles + generatorTypeFile
```

### `GeneratorTypeConverter.kt` — per-module marker hierarchy builder

**New file:** `src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorTypeConverter.kt`.

```kotlin
fun Module.convertToGeneratorType(): File
```

Collects every `Type`, `Enum`, `Union`, and `Refined` definition in the module (`Model` instances minus channels/endpoints) and emits a single IR `File` named `GeneratorType` whose element is a sealed `Interface` with one `Struct` (or equivalent singleton) per definition. Each language's per-language generator already handles sealed hierarchies for regular model emission; no generator-level changes are required.

### `EmitHelpers.kt` — string-subpackage overloads

Add string-subpackage overloads of `wrapWithPackage` and `wrapWithModuleImport`. Existing `Definition`-based overloads delegate to the new ones. This lets the generator hook route output to a literal `"generator"` subpackage without going through `Definition.namespace()` (which is reserved for routing `Definition`s to `model/`).

```kotlin
fun File.wrapWithPackage(
    packageName: PackageName,
    subPackage: String,
    wirespecImport: Element,
    needsImport: Boolean,
    nameTransform: (Name) -> String = { it.pascalCase() },
): File { /* same body as existing, using packageName + subPackage */ }

fun File.wrapWithPackage(
    packageName: PackageName,
    definition: Definition,
    wirespecImport: Element,
    needsImport: Boolean,
    nameTransform: (Name) -> String = { it.pascalCase() },
): File = wrapWithPackage(packageName, definition.namespace(), wirespecImport, needsImport, nameTransform)

// analogous overloads for wrapWithModuleImport
```

### Per-language emitter overrides

Each of the six emitters adds two overrides:

**`override fun emitGenerator(...)`** — produces one generator file per definition:
1. Dispatches on `Definition` kind → calls the matching `convertToGenerator()` overload.
2. Runs `sanitizeNames(sanitizationConfig)` and language-specific transforms.
3. Wraps with the `generator/` subpackage.

**`override fun emitGeneratorType(...)`** — produces the single per-module marker hierarchy file:
1. Calls `module.convertToGeneratorType()`.
2. Runs `sanitizeNames(sanitizationConfig)` and language-specific transforms.
3. Wraps with the `generator/` subpackage, same filename policy as generator files.

Subpackage wrapping per language (identical for both overrides):
- **Java, Kotlin, Scala** → `wrapWithPackage(packageName, subPackage = "generator", ...)`.
- **Python** → `wrapWithModuleImport(packageName, subPackage = "generator", ...)`.
- **Rust** → inline `subPackageName = packageName + "generator"`; filename uses snake_case.
- **TypeScript** → inline `subPackageName = PackageName("") + "generator"`; filename is PascalCase.

Regular model emission (the existing `emit(type, module)` / `emit(enum, module)` / …) is unchanged and still routes through `Definition.namespace()` into `model/`.

### File output

Each Type/Enum/Union/Refined definition produces a separate generator file, and each module produces one `GeneratorType` file:

| Language   | Model path                        | Generator path                                | GeneratorType path                             |
|------------|-----------------------------------|-----------------------------------------------|------------------------------------------------|
| Kotlin     | `…/generated/model/Address.kt`    | `…/generated/generator/AddressGenerator.kt`   | `…/generated/generator/GeneratorType.kt`       |
| Java       | `…/generated/model/Address.java`  | `…/generated/generator/AddressGenerator.java` | `…/generated/generator/GeneratorType.java`     |
| Scala      | `…/generated/model/Address.scala` | `…/generated/generator/AddressGenerator.scala`| `…/generated/generator/GeneratorType.scala`    |
| Python     | `…/generated/model/address.py`    | `…/generated/generator/address_generator.py`  | `…/generated/generator/generator_type.py`      |
| TypeScript | `…/generated/model/Address.ts`    | `…/generated/generator/AddressGenerator.ts`   | `…/generated/generator/GeneratorType.ts`       |
| Rust       | `…/generated/model/address.rs`    | `…/generated/generator/address_generator.rs`  | `…/generated/generator/generator_type.rs`      |

## Testing

Full coverage across all three levels, all six languages (strategy T1).

### Level 1 — IR unit tests (`commonTest`)

`src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/`:

- **`IrConverterTest.testSharedContainsGeneratorField`** — asserts `GeneratorField` interface exists and that ten sibling structs (`GeneratorFieldString`, `GeneratorFieldInteger`, `GeneratorFieldNumber`, `GeneratorFieldBoolean`, `GeneratorFieldBytes`, `GeneratorFieldEnum`, `GeneratorFieldUnion`, `GeneratorFieldArray`, `GeneratorFieldNullable`, `GeneratorFieldDict`) exist at the same level.
- **`GeneratorConverterTest`** — for each of Type, Enum, Union, Refined, and for Type variants exercising array, dict, nullable-primitive, and nullable-custom fields, assert the produced IR `File` matches the expected function body structure (`Function` signature, `Switch` cases for Union, `ConstructorStatement`'s `namedArguments` for Type, etc.). Each test also asserts the callback call-sites pass the correct `GeneratorType.Xxx` marker as the `type` argument.
- **`GeneratorTypeConverterTest`** — given a fixture module with a `Type`, `Enum`, `Union`, and `Refined`, assert the produced IR `File` is a sealed `Interface` named `GeneratorType` containing one child per definition (excluding endpoints and channels).

### Level 2 — Per-emitter snapshot tests

Each emitter's existing `…IrEmitterTest` gets:

1. Updated shared-runtime assertion to include the ten `GeneratorField` variants and the `Generator` interface.
2. New `testEmitGeneratorForType`, `testEmitGeneratorForEnum`, `testEmitGeneratorForUnion`, `testEmitGeneratorForRefined` methods asserting the generated source string for a fixed fixture — including the `GeneratorType.Xxx` marker at callback call-sites.
3. New `testEmitGeneratorForGeneratorFieldArray`, `testEmitGeneratorForGeneratorFieldDict`, `testEmitGeneratorForGeneratorFieldNullable` methods covering the structural-iteration cases.
4. New `testEmitGeneratorType` method asserting the `GeneratorType.{ext}` file is emitted as a sealed hierarchy of the module's definitions (Kotlin: `sealed interface`; Java: `sealed interface` with `record` permits; Scala: `sealed trait` + companion case objects; Rust: `pub enum`; TypeScript: string-literal union; Python: `str`-based `Enum`).

Expected-output strings are stored inline using `trimMargin` multiline strings (existing convention in each `…IrEmitterTest`).

### Level 3 — Docker verify tests (`src/verify/`)

**Fixtures** (`src/compiler/test/.../CompileGeneratorTest.kt`): a wirespec source containing a `Refined` (UUID), `Enum` (Color), `Union` (Shape), and a `Type` (Person) whose fields cover primitive, custom, array-of-custom, dict-of-primitive, nullable-primitive, and nullable-custom.

**Test programs** (`src/verify/src/test/kotlin/.../VerifyGeneratorTest.kt`, built via IR DSL and compiled per language through Docker):

1. Implement `Wirespec.Generator` with a deterministic callback that pattern-matches exhaustively on the `GeneratorType` marker and returns fixed values per type + field combination.
2. Call `PersonGenerator.generate(listOf("Person"), callback)`.
3. Assert the returned `Person` has the expected scalar field values, the nested `Address` populated correctly, the correct number of `addresses`, the nullable field populated/null consistent with the callback, and that `UUID` / `Color` / `Shape` satisfy their respective constraints.
4. Separately assert that the generated source compiles without any `when`/`match`/`switch` exhaustiveness warnings in languages where the compiler supports that check (Kotlin, Java, Scala, Rust).

Runs parameterized across Java, Kotlin, TypeScript, Python, Scala, Rust via existing verify infrastructure.

## Phasing

Five phases, each independently green-buildable:

1. **Shared runtime.** Add the `GeneratorField` sealed hierarchy and `Generator` interface to `SharedWirespec.convert()`. Update `IrConverterTest.testSharedContainsGeneratorField`. Update each language's existing `…IrEmitterTest` shared-runtime fixture to include the new block. No per-language generator code changes expected (flat siblings need no generator updates).
2. **`GeneratorTypeConverter` + marker-hierarchy emission.** Add `Module.convertToGeneratorType()`. Add `emitGeneratorType` to `IrEmitter` and override in each of the six language emitters. Add `GeneratorTypeConverterTest` and per-emitter `testEmitGeneratorType` snapshot tests.
3. **`GeneratorConverter`.** Add the four `convertToGenerator()` overloads + helpers (referencing `GeneratorType.Xxx` markers built in Phase 2). Add `GeneratorConverterTest` covering all definition kinds and field-type combinations.
4. **`emitGenerator` hook + per-emitter wiring.** Add `subPackage: String` overloads to `EmitHelpers`. Add `emitGenerator` to `IrEmitter`. Override in each of the six language emitters. Add per-emitter snapshot tests (`testEmitGeneratorForType/Enum/Union/Refined/GeneratorFieldArray/GeneratorFieldDict/GeneratorFieldNullable`).
5. **Docker verify.** Add `CompileGeneratorTest` fixture and `VerifyGeneratorTest` parameterized across all six languages.

## Out of Scope

- No removal or modification of `src/tools/generator/Generator.kt` (deprecate separately once downstream users have migrated).
- No endpoint/channel generator (only `Type`, `Enum`, `Union`, `Refined`).
- No rename to "Arbitrary".
- No nesting of `XxxField` variants under `GeneratorField` — they remain flat siblings.
- No custom regex-delimiter parsing beyond the existing `/pattern/flags` convention.
- No verify-time performance, parallelism, or infrastructure changes.

## Key File Reference

| File | Role |
|---|---|
| `src/compiler/ir/.../converter/IrConverter.kt` | Add `GeneratorField` hierarchy + `Generator` interface to `SharedWirespec.convert()`. |
| `src/compiler/ir/.../converter/GeneratorConverter.kt` | **New.** Four `convertToGenerator()` overloads + helpers. |
| `src/compiler/ir/.../converter/GeneratorTypeConverter.kt` | **New.** `Module.convertToGeneratorType()` — builds the per-module sealed marker hierarchy. |
| `src/compiler/ir/.../emit/IrEmitter.kt` | Add `emitGenerator` and `emitGeneratorType` hooks + thread their outputs through `emit(module, logger)`. |
| `src/compiler/ir/.../emit/EmitHelpers.kt` | Add `subPackage: String` overloads of `wrapWithPackage` and `wrapWithModuleImport`. |
| `src/compiler/emitters/{java,kotlin,python,rust,scala,typescript}/.../XxxIrEmitter.kt` | Override `emitGenerator` and `emitGeneratorType`; route output to `generator/` subpackage. |
| `src/compiler/ir/.../converter/IrConverterTest.kt` | Add `testSharedContainsGeneratorField`. |
| `src/compiler/ir/.../converter/GeneratorConverterTest.kt` | **New.** IR-level converter tests. |
| `src/compiler/ir/.../converter/GeneratorTypeConverterTest.kt` | **New.** Per-module marker-hierarchy tests. |
| `src/compiler/emitters/*/.../XxxIrEmitterTest.kt` | Update shared-runtime fixture; add `testEmitGeneratorForXxx` and `testEmitGeneratorType` methods. |
| `src/compiler/test/.../CompileGeneratorTest.kt` | **New.** Verify fixture wirespec source. |
| `src/verify/.../VerifyGeneratorTest.kt` | **New.** Docker-based cross-language runtime verification. |
