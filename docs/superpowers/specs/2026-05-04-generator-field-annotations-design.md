# Field Annotations on the Arbitrary-Data Generator Callback

**Status:** Design
**Date:** 2026-05-04
**Branch:** `ir-arbitrary`

## Problem

The IR-emitted `*Generator.kt` (and per-language equivalent) factories drive
arbitrary-data generation by calling back into a user-supplied
`Wirespec.Generator`:

```kotlin
interface Generator {
  fun <T : Any> generate(path: List<String>, type: KType, field: GeneratorField<T>): T
}
```

Field annotations from the `.ws` source — already parsed into `Field.annotations:
List<Annotation>` — are silently discarded by `GeneratorConverter`. A custom
`Generator` cannot see them, so it cannot specialise its output for fields
marked `@Email`, `@Range(min: 0, max: 100)`, `@Hostname`, etc.

## Goal

Surface raw, per-field annotations to the runtime `Generator.generate(...)`
callback in every language emitter (Kotlin, Java, TypeScript, Python, Rust),
without imposing built-in semantics on any annotation. The user decides what
each annotation means.

## Non-goals

- No converter-side interpretation of well-known annotations (e.g. mapping
  `@Email` to a regex). The pipeline is a pass-through.
- No type-level or endpoint-level annotations — only field annotations on
  `type { ... }` shapes.
- No backwards-compatible overload of the old 3-arg `generate()`. The
  `ir-arbitrary` branch is recent and the only known custom implementation in
  the repo is `SeededGenerator` in the spring-boot example.

## API shape

A new fourth parameter is added to the `Generator.generate()` callback in
every per-language Wirespec runtime. Annotations are represented as plain
language-native data — lists, maps, and primitive values — with no dedicated
`Annotation` value type (deliberately: keeps the runtime DSL surface minimal).

Each annotation is `Map<String, Any?>` with two reserved keys:

- `"name"` — annotation name as a `String` (e.g. `"Range"`).
- `"parameters"` — nested `Map<String, Any?>` of parameter name → coerced
  value. Empty map when the annotation has no parameters.

Annotations are passed as a `List` to preserve source order and allow
duplicate-named annotations.

### Per-language signatures

**Kotlin**
```kotlin
interface Generator {
  fun <T : Any> generate(
    path: List<String>,
    type: KType,
    field: GeneratorField<T>,
    annotations: List<Map<String, Any?>>,
  ): T
}
```

**Java**
```java
public interface Generator {
  <T> T generate(
    List<String> path,
    java.lang.reflect.Type type,
    GeneratorField<T> field,
    List<Map<String, Object>> annotations
  );
}
```

**TypeScript**
```typescript
interface Generator {
  generate<T>(
    path: string[],
    type: unknown,
    field: GeneratorField<T>,
    annotations: Array<Record<string, unknown>>,
  ): T;
}
```

**Python**
```python
class Generator(Protocol):
    def generate(
        self,
        path: list[str],
        type: type,
        field: GeneratorField[T],
        annotations: list[dict[str, Any]],
    ) -> T: ...
```

**Rust** — Rust cannot express a heterogeneous map without a dependency, and
the project mandates dependency-free generated code. To keep typed primitives
while staying dep-free, Rust gets a small tagged enum. This is the only
language that deviates from the dynamic-map shape. The enum lives alongside
the existing `GeneratorField*` types in the Rust shared runtime.

```rust
pub enum AnnotationValue {
    Bool(bool),
    Int(i64),
    Float(f64),
    Str(String),
    Sequence(Vec<AnnotationValue>),
    Mapping(BTreeMap<String, AnnotationValue>),
}
```

The Rust `Generator` trait's `generate` function gains a final parameter
`annotations: Vec<BTreeMap<String, AnnotationValue>>` (matching the order and
type-style of the other four languages). The other parameters (`path`,
type-identifier, `field`) keep whatever shape the existing Rust generator
trait already uses — this spec does not redefine them.

For Rust the per-annotation map keys are still `"name"` (yielding
`AnnotationValue::Str`) and `"parameters"` (yielding `AnnotationValue::Mapping`).

### Worked example

`.ws`:

```
type Foo {
  @Range(min: 0, max: 1.5, label: hello, active: true) @Deprecated
  age: Integer
}
```

Reaches the Kotlin callback as:

```kotlin
listOf(
  mapOf(
    "name" to "Range",
    "parameters" to mapOf(
      "min" to 0L,
      "max" to 1.5,
      "label" to "hello",
      "active" to true,
    ),
  ),
  mapOf(
    "name" to "Deprecated",
    "parameters" to emptyMap<String, Any?>(),
  ),
)
```

## Coercion rules

The parser stores every literal as `Annotation.Value.Single(String)` —
including numerics. `GeneratorConverter` applies the following rules when
building the IR `MapExpression` for each parameter value:

| Parsed string                              | Emitted as                                   |
|--------------------------------------------|----------------------------------------------|
| `"true"` or `"false"`                      | native `Boolean`                             |
| matches `^-?[0-9]+$`                       | native 64-bit signed integer                 |
| matches `^-?[0-9]+\.[0-9]+([eE]-?[0-9]+)?$` | native 64-bit float                          |
| anything else                              | native `String`                              |

`Annotation.Value.Array` and `Annotation.Value.Dict` recurse with the same
rules. Booleans are matched case-sensitively (`"True"` becomes a `String`,
not a `Boolean`) — strict matching avoids ambiguity.

Per-language native types: Kotlin `Long`/`Double`/`Boolean`/`String`; Java
boxed `Long`/`Double`/`Boolean`/`String`; TypeScript `number`/`boolean`/`string`;
Python `int`/`float`/`bool`/`str`; Rust `AnnotationValue::Int`/`Float`/`Bool`/`Str`.

## Implementation

### Stage 1 — IR converter (`GeneratorConverter.kt`)

`src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/GeneratorConverter.kt`

`TypeWirespec.convertToGenerator()` constructs the per-field
`generator.generate(...)` `FunctionCall`. The change:

1. For each `Field`, walk `field.annotations`. For each annotation, build
   an IR `MapExpression` with two entries:
   - `"name"` → `Literal(annotation.name, Type.String)`
   - `"parameters"` → recursive `MapExpression` over `annotation.parameters`,
     applying the coercion rules to each `Value.Single`, recursing into
     `Value.Array` (as IR list constructor) and `Value.Dict` (as nested
     `MapExpression`).
2. Wrap the per-annotation maps in an IR list-constructor producing
   `List<Map<String, Any?>>`.
3. Append it as the fourth argument to the existing `generator.generate(...)`
   call.

Add a private helper `Annotation.toIrMapExpression()` next to
`ReferenceWirespec.toFieldDescriptor()` in the same file.

### Stage 2 — Runtime emission (`IrConverter.SharedWirespec.convert()`)

`src/compiler/ir/src/commonMain/kotlin/community/flock/wirespec/ir/converter/IrConverter.kt`

The shared `Wirespec` runtime is itself emitted by this function. Add the
Rust-only `AnnotationValue` enum to the Rust shared runtime alongside the
existing `GeneratorField*` types. The other four languages need no new
types — annotations are plain `List<Map<...>>`.

### Stage 3 — `Generator` interface signature update

In each per-language Wirespec runtime emitter, add the new `annotations`
parameter to the `Generator` interface signature.

### Stage 4 — Example update

`examples/gradle-spring-boot/src/test/kotlin/community/flock/wirespec/examples/spring/testutil/TestGenerators.kt`

Add the new `annotations` parameter to `SeededGenerator.generate(...)`. Demo
its use by branching on at least one annotation:

```kotlin
override fun <T : Any> generate(
    path: List<String>,
    type: KType,
    field: Wirespec.GeneratorField<T>,
    annotations: List<Map<String, Any?>>,
): T {
    if (annotations.any { it["name"] == "Email" }) {
        @Suppress("UNCHECKED_CAST")
        return "user-${counter++}@example.com" as T
    }
    // ...existing dispatch on `field`
}
```

`examples/gradle-spring-boot/src/main/wirespec/projects.ws`: add `@Email`
to one field on `MemberInput` to exercise the end-to-end flow.

## Testing

### Compiler tests

New test file: `src/compiler/ir/src/commonTest/.../GeneratorConverterAnnotationTest.kt`

Cases:

1. Field with no annotations → fourth arg is empty list.
2. Field with single bare annotation `@Deprecated` → list of one map with
   `name="Deprecated"`, `parameters=emptyMap()`.
3. Field with parameterised annotation containing one of each coerced type:
   string, integer, decimal, boolean. Verify each value's literal type in
   the emitted IR.
4. Field with `Value.Array` parameter `[1, 2, 3]` → list of three `Long`s.
5. Field with `Value.Dict` parameter `{a: 1, b: hello}` → nested map.
6. Field with two annotations of the same name (e.g.
   `@Validate(min: 0) @Validate(max: 100)`) → both preserved, in source order.

### Runtime / verify tests

`src/verify/.../VerifyGeneratorTest.kt` (or its current location) — extend
the existing Kotlin runtime check with a fixture whose `.ws` source has at
least one annotation, asserting the annotation list reaches the callback
intact.

### Example test

The spring-boot example tests already round-trip the generators; the only
required change is updating `SeededGenerator` to accept the fourth parameter.
The new `@Email` field demonstrates user-side branching.

## Migration notes

This is a breaking change to the `Wirespec.Generator` interface in five
languages. The only known consumer in the repo is `SeededGenerator` in
`examples/gradle-spring-boot`. External users with custom `Generator`
implementations on the `ir-arbitrary` branch must add the fourth parameter
to their `generate(...)` override.

## Key files

| File | Change |
|------|--------|
| `src/compiler/ir/.../converter/GeneratorConverter.kt` | Emit annotation list at every `generate(...)` call site; add `Annotation.toIrMapExpression()` helper. |
| `src/compiler/ir/.../converter/IrConverter.kt` | Add Rust-only `AnnotationValue` enum to shared runtime. Update `Generator` interface signature in all five languages. |
| `src/compiler/ir/src/commonTest/.../GeneratorConverterAnnotationTest.kt` | New — coercion + structure tests. |
| `examples/gradle-spring-boot/src/main/wirespec/projects.ws` | Add `@Email` on one `MemberInput` field. |
| `examples/gradle-spring-boot/src/test/kotlin/.../testutil/TestGenerators.kt` | Update `SeededGenerator` to consume `annotations`. |
