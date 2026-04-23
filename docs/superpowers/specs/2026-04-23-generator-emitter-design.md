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
    fun <T> generate(path: List<String>, field: GeneratorField<T>): T
}
```

Parameters:
- `path: List<String>` — segments accumulated as the generator recurses, e.g. `["Person", "addresses", "0", "street"]`.
- `field: GeneratorField<T>` — describes what value is needed, with constraints. The generic `T` is inferred; no casts required in callback bodies.

The callback is named `generator` at the call site, but the **parameter name** inside generated functions is `callback` to avoid visually overloading the word "generator" in function bodies. (The parameter name is emitted by `GeneratorConverter` and is purely cosmetic.)

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
        street = callback.generate(path + "street",
            Wirespec.GeneratorFieldString(regex = null)),
        number = callback.generate(path + "number",
            Wirespec.GeneratorFieldInteger(min = null, max = null)),
        postalCode = UUIDGenerator.generate(path + "postalCode", callback),
    )
}
```

Field handling by `Reference`:
- **Primitive** → `callback.generate(path + fieldName, <Primitive-to-XxxField>)`.
- **Custom** → `{FieldTypeName}Generator.generate(path + fieldName, callback)` — recursive composition.
- **Iterable** →
  ```kotlin
  val count = callback.generate(path + fieldName, Wirespec.GeneratorFieldArray(inner = <innerDesc>))
  val values = (0 until count).map { i ->
      <recurse-on-inner>(path + fieldName + i.toString(), callback)
  }
  ```
- **Dict** →
  ```kotlin
  val count = callback.generate(path + fieldName,
      Wirespec.GeneratorFieldDict(key = <keyDesc>, value = <valueDesc>))
  val entries = (0 until count).associate { i ->
      <keyGen>(path + fieldName + "key$i", callback) to
      <valueGen>(path + fieldName + "value$i", callback)
  }
  ```
- **Nullable reference** (any of the above with `isNullable = true`) → wrap the value production:
  ```kotlin
  val isNull = callback.generate(path + fieldName,
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
        value = callback.generate(path + "value",
            Wirespec.GeneratorFieldString(regex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")),
    )
}
```

Integer/Number refined types emit `GeneratorFieldInteger(min, max)` / `GeneratorFieldNumber(min, max)` with bounds from the refinement constraint.

#### Enum

```kotlin
object ColorGenerator {
    fun generate(path: List<String>, callback: Wirespec.Generator): Color = Color(
        label = callback.generate(path + "value",
            Wirespec.GeneratorFieldEnum(values = listOf("RED", "GREEN", "BLUE"))),
    )
}
```

Assumes each language's enum emission produces a `label: String` constructor parameter — this is already the convention across current Wirespec emitters.

#### Union

```kotlin
object ShapeGenerator {
    fun generate(path: List<String>, callback: Wirespec.Generator): Shape {
        val variant = callback.generate(path + "variant",
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

Add:
```kotlin
fun emitGenerator(definition: Definition, module: Module): File? = null
```

Extend `emit(module, logger)` so that after assembling `definitionFiles + clientFiles`, it collects:
```kotlin
val generatorFiles = module.statements.toList()
    .filterIsInstance<Model>()
    .mapNotNull { model ->
        logger.info("Emitting Generator for ${model::class.simpleName} ${model.identifier.value}")
        emitGenerator(model, module)
    }
return definitionFiles + clientFiles + generatorFiles
```

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

Each of the six emitters adds an `override fun emitGenerator(...)` that:

1. Dispatches on `Definition` kind → calls the matching `convertToGenerator()` overload.
2. Runs `sanitizeNames(sanitizationConfig)` and language-specific transforms.
3. Wraps with the `generator/` subpackage:
   - **Java, Kotlin, Scala** → `wrapWithPackage(packageName, subPackage = "generator", ...)`.
   - **Python** → `wrapWithModuleImport(packageName, subPackage = "generator", ...)`.
   - **Rust** → inline `subPackageName = packageName + "generator"`; filename uses snake_case.
   - **TypeScript** → inline `subPackageName = PackageName("") + "generator"`; filename is PascalCase.

Regular model emission (the existing `emit(type, module)` / `emit(enum, module)` / …) is unchanged and still routes through `Definition.namespace()` into `model/`.

### File output

Each Type/Enum/Union/Refined definition produces a separate generator file:

| Language   | Model path                           | Generator path                               |
|------------|--------------------------------------|----------------------------------------------|
| Kotlin     | `…/generated/model/Address.kt`      | `…/generated/generator/AddressGenerator.kt`  |
| Java       | `…/generated/model/Address.java`    | `…/generated/generator/AddressGenerator.java`|
| Scala      | `…/generated/model/Address.scala`   | `…/generated/generator/AddressGenerator.scala`|
| Python     | `…/generated/model/address.py`      | `…/generated/generator/address_generator.py` |
| TypeScript | `…/generated/model/Address.ts`      | `…/generated/generator/AddressGenerator.ts`  |
| Rust       | `…/generated/model/address.rs`      | `…/generated/generator/address_generator.rs` |

## Testing

Full coverage across all three levels, all six languages (strategy T1).

### Level 1 — IR unit tests (`commonTest`)

`src/compiler/ir/src/commonTest/kotlin/community/flock/wirespec/ir/converter/`:

- **`IrConverterTest.testSharedContainsGeneratorField`** — asserts `GeneratorField` interface exists and that ten sibling structs (`GeneratorFieldString`, `GeneratorFieldInteger`, `GeneratorFieldNumber`, `GeneratorFieldBoolean`, `GeneratorFieldBytes`, `GeneratorFieldEnum`, `GeneratorFieldUnion`, `GeneratorFieldArray`, `GeneratorFieldNullable`, `GeneratorFieldDict`) exist at the same level.
- **`GeneratorConverterTest`** — for each of Type, Enum, Union, Refined, and for Type variants exercising array, dict, nullable-primitive, and nullable-custom fields, assert the produced IR `File` matches the expected function body structure (`Function` signature, `Switch` cases for Union, `ConstructorStatement`'s `namedArguments` for Type, etc.).

### Level 2 — Per-emitter snapshot tests

Each emitter's existing `…IrEmitterTest` gets:

1. Updated shared-runtime assertion to include the ten `GeneratorField` variants and the `Generator` interface.
2. New `testEmitGeneratorForType`, `testEmitGeneratorForEnum`, `testEmitGeneratorForUnion`, `testEmitGeneratorForRefined` methods asserting the generated source string for a fixed fixture.
3. New `testEmitGeneratorForGeneratorFieldArray`, `testEmitGeneratorForGeneratorFieldDict`, `testEmitGeneratorForGeneratorFieldNullable` methods covering the structural-iteration cases.

Expected-output strings are stored inline using `trimMargin` multiline strings (existing convention in each `…IrEmitterTest`).

### Level 3 — Docker verify tests (`src/verify/`)

**Fixtures** (`src/compiler/test/.../CompileGeneratorTest.kt`): a wirespec source containing a `Refined` (UUID), `Enum` (Color), `Union` (Shape), and a `Type` (Person) whose fields cover primitive, custom, array-of-custom, dict-of-primitive, nullable-primitive, and nullable-custom.

**Test programs** (`src/verify/src/test/kotlin/.../VerifyGeneratorTest.kt`, built via IR DSL and compiled per language through Docker):

1. Implement `Wirespec.Generator` with a deterministic callback that returns fixed values based on `path + field-type`.
2. Call `PersonGenerator.generate(listOf("Person"), callback)`.
3. Assert the returned `Person` has the expected scalar field values, the nested `Address` populated correctly, the correct number of `addresses`, the nullable field populated/null consistent with the callback, and that `UUID` / `Color` / `Shape` satisfy their respective constraints.

Runs parameterized across Java, Kotlin, TypeScript, Python, Scala, Rust via existing verify infrastructure.

## Phasing

Four phases, each independently green-buildable:

1. **Shared runtime.** Add the `GeneratorField` sealed hierarchy and `Generator` interface to `SharedWirespec.convert()`. Update `IrConverterTest.testSharedContainsGeneratorField`. Update each language's existing `…IrEmitterTest` shared-runtime fixture to include the new block. No per-language generator code changes expected (flat siblings need no generator updates).
2. **`GeneratorConverter`.** Add the four `convertToGenerator()` overloads + helpers. Add `GeneratorConverterTest` covering all definition kinds and field-type combinations.
3. **`emitGenerator` hook + per-emitter wiring.** Add `subPackage: String` overloads to `EmitHelpers`. Add `emitGenerator` to `IrEmitter`. Override in each of the six language emitters. Add per-emitter snapshot tests (`testEmitGeneratorForType/Enum/Union/Refined/GeneratorFieldArray/GeneratorFieldDict/GeneratorFieldNullable`).
4. **Docker verify.** Add `CompileGeneratorTest` fixture and `VerifyGeneratorTest` parameterized across all six languages.

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
| `src/compiler/ir/.../emit/IrEmitter.kt` | Add `emitGenerator` hook + thread `generatorFiles` through `emit(module, logger)`. |
| `src/compiler/ir/.../emit/EmitHelpers.kt` | Add `subPackage: String` overloads of `wrapWithPackage` and `wrapWithModuleImport`. |
| `src/compiler/emitters/{java,kotlin,python,rust,scala,typescript}/.../XxxIrEmitter.kt` | Override `emitGenerator`; route output to `generator/` subpackage. |
| `src/compiler/ir/.../converter/IrConverterTest.kt` | Add `testSharedContainsGeneratorField`. |
| `src/compiler/ir/.../converter/GeneratorConverterTest.kt` | **New.** IR-level converter tests. |
| `src/compiler/emitters/*/.../XxxIrEmitterTest.kt` | Update shared-runtime fixture; add `testEmitGeneratorForXxx` methods. |
| `src/compiler/test/.../CompileGeneratorTest.kt` | **New.** Verify fixture wirespec source. |
| `src/verify/.../VerifyGeneratorTest.kt` | **New.** Docker-based cross-language runtime verification. |
