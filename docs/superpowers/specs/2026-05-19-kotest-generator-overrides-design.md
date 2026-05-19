# Kotest Generator Overrides — Design

**Date:** 2026-05-19
**Scope:** `src/integration/kotest` — `kotestWirespecKotlinGenerator` (and, by sharing the same builder, the Java/Scala/JS facades).

## Goal

Give users a `register*` interface on `KotestWirespecGeneratorBuilder` that lets them override field generation:

1. **By `(parent type, field name)`** — e.g. always produce a specific value for `User.email`, leaving `Order.email` untouched.
2. **By absolute path with `*` wildcard** — e.g. `("users", "*", "id")` to override `users[i].id` for every `i`.

These two scoped overrides **replace** the existing `register(name)` / `@Generator("name")` lookup. The `@Seed` regeneration machinery stays in place.

## Removal: `@Generator` lookup

The previous `@Generator("name")` mechanism — `register(name) { Arb<String> }` on the builder, the `DEFAULT_ARBS` catalog of preinstalled arbs, and the `annotations.namedGeneratorOrNull()` lookup inside `KotestWirespecGenerator` — is removed. Any `@Generator(...)` annotation that the IR still emits is ignored by this generator. Users who want kotest's `Arb.email()` / `Arb.uuid()` / etc. now wire them up explicitly via `registerField<Parent>("email") { Arb.email() }` or `registerPath("users", "*", "id") { Arb.uuid() }`.

Concretely, this means deleting:
- `KotestWirespecGeneratorBuilder.register(name, factory)`
- `KotestWirespecGenerator.namedArbs` and `namedGeneratorOrNull(...)`
- `DefaultArbs.kt` (the `DEFAULT_ARBS` map and `uuidArb()`)
- The `@Generator`-related tests in `KotestWirespecGeneratorTest`, `DefaultArbsTest`, and the JVM adapter tests

Existing scoped behavior (`@Seed`) is **not** affected.

## Non-goals

- Single-axis matching ("any field named `email` everywhere" or "any field of type `EmailAddress`"). Both intentionally rejected — every override is scoped.
- Path predicates. Rejected in favor of literal segments + `*`.
- JS ergonomics for reified `Parent`. JS users can fall back to the string-keyed `registerFieldByTypeName(...)` underneath.
- Backwards compatibility with `register(name)`. It is removed in this change.

## Public API

The builder exposes only the two new scoped overrides plus the seed parameter:

```kotlin
kotestWirespecKotlinGenerator(seed = 1L) {
    registerField<User>("email") { Arb.email() }        // by (parent type, field name)
    registerField<User>("age", 42L)                     // value shorthand
    registerPath("users", "*", "id") { Arb.uuid() }     // by absolute path
    registerPath("orders", "0", "total", value = 100L)  // value shorthand
}
```

### Signatures

**commonMain (`KotestWirespecGenerator.kt`):**

```kotlin
class KotestWirespecGeneratorBuilder internal constructor() {
    internal val overrides: OverrideRegistry = OverrideRegistry()

    fun registerFieldByTypeName(
        typeName: String,
        name: String,
        factory: () -> Arb<*>,
    )

    fun registerFieldByTypeName(
        typeName: String,
        name: String,
        value: Any?,
    )

    fun registerPath(vararg segments: String, factory: () -> Arb<*>)
    fun registerPath(vararg segments: String, value: Any?)
}
```

**jvmMain (`KotestBuilderJvm.kt`):**

```kotlin
inline fun <reified Parent : Any> KotestWirespecGeneratorBuilder.registerField(
    name: String,
    noinline factory: () -> Arb<*>,
) = registerFieldByTypeName(typeOf<Parent>().toString(), name, factory)

inline fun <reified Parent : Any> KotestWirespecGeneratorBuilder.registerField(
    name: String,
    value: Any?,
) = registerFieldByTypeName(typeOf<Parent>().toString(), name, value)
```

### Why `Arb<*>` and not `Arb<Value>`

The produced value's type depends on context:
- For a primitive field (`KotestFieldString`/`Integer*`/…), it is the primitive type.
- For a Refined field, the user provides the inner primitive and we auto-wrap (see "Auto-wrap" below).

Trying to thread `Value` through the type system would force users into ugly two-parameter forms (`registerField<User, String>("email") { … }`) and still wouldn't capture Refined wrapping. `Arb<*>` is checked at draw time with a clear error message — the same trade-off the rest of this module already makes (cf. unchecked casts in `KotestWirespecGenerator.generate`).

### Value shorthand

Both `registerField` and `registerPath` have a value overload. It wraps in `Arb.constant(value)` internally so the runtime has a single code path.

## Matching & precedence

### Parent-shape stack

`KotestWirespecGenerator` already tracks `shapeDepth: Int` to gate `@Seed`-on-primitive precedence. We **add** an independent `parentStack` of `ParentFrame(typeName)` to track the enclosing shape:

```kotlin
private data class ParentFrame(val typeName: String)
private val parentStack = ArrayDeque<ParentFrame>()
```

- Push on entry to `KotestFieldShape` handling (both `generateLeaf` and `generateSeededShape`), pop on exit.
- `KotestFieldArray`, `KotestFieldNullable`, `KotestFieldDict` are transparent — they do **not** push. A leaf inside `users[0].address.zip` still sees `Address` as immediate parent, and `User` further up.
- `shapeDepth` is **not** replaced. It keeps its existing role: it is only incremented in `generateSeededShape` (capture pass + pending-seed branch), which is the precise gate the current `@Seed` semantics rely on. `parentStack` is purely for parent-type lookup.

### Lookup order

At every `generate(path, field)` call:

```
1. @Seed capture/consume           (unchanged — structural, not a value override)
2. @Seed on primitive (top level)  (unchanged)
3. @Seed-driven seeded shape       (unchanged)
4. path override                   NEW — most specific pattern wins
5. field override                  NEW — keyed by (parentStack.peek(), path.last())
6. KotestField default leaf        (unchanged)
```

There is no `@Generator(name)` step anymore.

### Path matching

```kotlin
internal sealed interface PathSegment {
    data class Literal(val value: String) : PathSegment
    data object Wildcard : PathSegment
}

internal data class PathPattern(val segments: List<PathSegment>) {
    val specificity: Int = segments.count { it is PathSegment.Literal }

    fun matches(path: List<String>): Boolean {
        if (path.size != segments.size) return false
        return segments.zip(path).all { (seg, p) ->
            when (seg) {
                is PathSegment.Literal -> seg.value == p
                PathSegment.Wildcard -> true
            }
        }
    }
}
```

- Patterns are compiled once at registration time.
- When multiple patterns match a path, the pattern with the highest `specificity` (fewest wildcards) wins.
- Two equally-specific patterns matching the same path are an error raised at lookup time: `"Ambiguous path overrides for $path: $pattern1 and $pattern2"`. Detection is cheap because we scan only the registered patterns.

### Field matching

```kotlin
internal data class FieldKey(val parentTypeName: String, val fieldName: String)

internal class OverrideRegistry {
    val pathOverrides: MutableList<Pair<PathPattern, () -> Arb<*>>> = mutableListOf()
    val fieldOverrides: MutableMap<FieldKey, () -> Arb<*>> = mutableMapOf()
}
```

At lookup, after path overrides:

```kotlin
val parent = parentStack.lastOrNull() ?: return null
val name = path.lastOrNull() ?: return null
return overrides.fieldOverrides[FieldKey(parent.typeName, name)]
```

If `parentStack` is empty (no enclosing shape — e.g. a top-level Refined `gen.generate(path, refinedShape)`), the field rule simply does not fire. Use a `registerPath` if you need to target that case.

Re-registering an existing `FieldKey` throws — same posture as the path-ambiguity check.

## Auto-wrap for Refined fields

The IR-emitted Refined class always has a single-arg constructor wrapping a primitive (`String`, `Long`, `Int`, `Double`, `Float`, `Boolean`, `ByteArray`). When an override fires and the underlying `KotestField` is a `KotestFieldShape<T>` whose `type` is a Refined, we wrap the drawn value with the Refined's constructor.

```kotlin
// jvmMain
private val refinedCtorCache = ConcurrentHashMap<KType, KFunction<Any>?>()

private fun refinedCtorFor(type: KType): KFunction<Any>? = refinedCtorCache.getOrPut(type) {
    val cls = (type.classifier as? KClass<*>) ?: return@getOrPut null
    @Suppress("UNCHECKED_CAST")
    (cls.constructors.singleOrNull()?.takeIf { it.parameters.size == 1 } as? KFunction<Any>)
}
```

The JVM `RefinedWrapper` implementation (installed via the SPI described in "File layout"):

```kotlin
internal object JvmRefinedWrapper : RefinedWrapper {
    override fun wrap(drawn: Any?, field: KotestField<*>, path: List<String>): Any? {
        val ctor = (field as? KotestFieldShape<*>)?.type?.let(::refinedCtorFor) ?: return drawn
        return try {
            ctor.call(drawn)
        } catch (e: IllegalArgumentException) {
            error(
                "Override at ${path.joinToString("/")}: expected " +
                "Arb<${ctor.parameters[0].type}> for refined " +
                "${((field.type.classifier) as KClass<*>).qualifiedName}, " +
                "got value of type ${drawn?.let { it::class.qualifiedName }}",
            )
        }
    }
}
```

This is JVM-only because it uses `kotlin.reflect.full`. JS-facade auto-wrap, if needed later, can install its own `RefinedWrapper`.

## File layout

```
src/integration/kotest/
├─ src/commonMain/.../
│  ├─ KotestField.kt                  (unchanged)
│  ├─ KotestWirespecGenerator.kt      (change: parent-shape stack; @Generator
│  │                                   lookup + namedArbs field removed; two
│  │                                   new lookup hooks at the precedence points)
│  ├─ KotestOverrides.kt              NEW — PathSegment, PathPattern, FieldKey,
│  │                                   OverrideRegistry (pure data, no reflection)
│  └─ DefaultArbs.kt                  DELETED
│
└─ src/jvmMain/.../
   ├─ KotestWirespecKotlinGenerator.kt  (unchanged adapter)
   └─ KotestBuilderJvm.kt               NEW — reified `registerField<Parent>`
                                        extensions + Refined ctor cache + auto-wrap
                                        hook (installed via the RefinedWrapper SPI)
```

Auto-wrap crosses the commonMain/jvmMain boundary via an SPI:

```kotlin
// commonMain
internal fun interface RefinedWrapper {
    fun wrap(drawn: Any?, field: KotestField<*>, path: List<String>): Any?
}

internal object IdentityRefinedWrapper : RefinedWrapper {
    override fun wrap(drawn: Any?, field: KotestField<*>, path: List<String>) = drawn
}
```

`KotestWirespecGenerator`'s constructor takes a `RefinedWrapper` (default = identity). The JVM `kotestWirespecKotlinGenerator(...)` factory passes `JvmRefinedWrapper` (defined in jvmMain) when constructing the underlying `KotestWirespecGenerator`. JS keeps the identity wrapper — JS callers don't have a Refined-class concept anyway. The override draw site calls `wrapper.wrap(drawn, field, path)` on every successful override hit.

## Testing

### commonTest — `KotestOverrideTest.kt`

- Path override fires at exact path.
- Path override with `*` matches every array index in `users[i].id`.
- Path override does **not** fire on shorter / longer paths.
- More specific pattern wins over a wildcard pattern at the same path.
- Two equally-specific overlapping path patterns → error at lookup.
- Field override fires only when the leaf is a direct child of a `KotestFieldShape` whose `type.toString()` matches.
- Field override does **not** fire for the same field name nested in a different parent type.
- Field override does **not** fire at the top level (no enclosing shape).
- Precedence: `@Seed` beats path; path beats field; field beats default leaf.

All commonTest assertions use string-keyed `registerFieldByTypeName(...)` so no reflection is involved.

### jvmTest — `KotestWirespecKotlinGeneratorOverrideJvmTest.kt`

- Reified `registerField<User>("email") { Arb.constant("a@b.com") }` passes through to a `String` field.
- Refined auto-wrap: given a generated `EmailAddress(val value: String)`, `registerField<User>("email") { Arb.constant("a@b.com") }` returns an `EmailAddress` instance with `value == "a@b.com"`.
- Type mismatch: `registerField<User>("email") { Arb.long(0..10) }` against a `String`-backed Refined throws with the documented error message.
- Value-shorthand overloads (`registerField<User>("age", 42L)`, `registerPath("users", "0", "id", value = "x")`) work end-to-end through the `WirespecKotlinGeneratorAdapter`.

### Removed tests

- `DefaultArbsTest` — deleted along with `DefaultArbs.kt`.
- `@Generator`-related cases in `KotestWirespecGeneratorTest`:
  - `Generator annotation routes to a registered Arb`
  - `Generator annotation lookup is case-insensitive`
  - `unknown Generator name throws a clear error`
- `@Generator`-related cases in `KotestWirespecKotlinGeneratorJvmTest`:
  - `adapter routes Wirespec_GeneratorFieldString through the algorithm`

`@Seed`-related tests stay green — that machinery is untouched.

## Risks & mitigations

| Risk | Mitigation |
|---|---|
| Downstream consumers depend on the removed `register(name)` API. | This is a deliberate breaking change. Migration is mechanical: replace `register("orderId") { Arb.uuid() }` with `registerField<Owner>("orderId") { Arb.uuid() }` or `registerPath(...)`. Call out in the change description so consumers update at the same time. |
| Refined ctor lookup picks up a synthetic constructor on some toolchain. | We require `constructors.singleOrNull() && parameters.size == 1`. Generated Refined classes have exactly one constructor — anything else returns null and we fall through to identity. |
| Users register a path override that shadows an `@Seed` field unintentionally. | `@Seed` stays at precedence 1–3, above path overrides — by design. Documented in the spec and in the builder's KDoc. |
| Stringly-typed `parent.type.toString()` differs across Kotlin versions. | Both sides use the same `typeOf<T>().toString()` representation, so they match identically. If this ever breaks, the test `Field override fires only when the leaf is a direct child of a matching KotestFieldShape` will fail loudly. |
| Two equally-specific path patterns at registration. | Detect ambiguity lazily at lookup time (when both match a real path), with an error message listing both patterns. |
