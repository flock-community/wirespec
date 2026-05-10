# Kotest integration for Java- and Scala-emitted code

**Date:** 2026-05-10
**Status:** Design approved

## Goal

Let Kotlin tests drive Java- and Scala-emitted `*Generator.generate(generator,
path)` factories using kotest property arbs, mirroring the existing
`kotestWirespecGenerator(...)` flow that today only works against
Kotlin-emitted code.

After this change, a Kotlin test in a project whose codegen target is Java or
Scala can write the same kind of seeded property test that already works for
Kotlin-emitted projects:

```kotlin
val gen = kotestWirespecJavaGenerator(seed = 1L) {
    register("orderId") { Arb.int(0..999_999).map { "ORD-%06d".format(it) } }
}
val project: Project = ProjectGenerator.generate(gen, emptyList())
```

## Non-goals

- Writing tests *in* Java or Scala (Kotest's DSL stays the test surface).
- Shipping a `wirespec-scala` runtime artifact. Scala consumers must
  `--emit-shared` so that `community.flock.wirespec.scala.Wirespec` lives in
  their generated source set; the kotest-scala adapter resolves that interface
  at runtime via reflection.
- Touching the `js` target. The new adapters are JVM-only.
- Generating any per-project glue code (no new emitter mode).
- Adding new Gradle subprojects.

## Architecture

Three new pieces, all on the JVM, all in the existing `:src:integration:kotest`
module:

```
┌──────────────────────────────────────────────────────────────────┐
│  src/integration/kotest  (existing KMP module — JVM + JS)        │
│                                                                   │
│  commonMain  KotestGenerator + KotestField* (unchanged)          │
│              kotestGenerator(seed, block) (unchanged)            │
│                                                                   │
│  jvmMain     KotestWirespecKotlinGenerator.kt (renamed from Jvm) │
│              KotestWirespecJavaGenerator.kt   (new)              │
│              KotestWirespecScalaGenerator.kt  (new)              │
│              ScalaInterop.kt                  (new helper)       │
└──────────────────────────────────────────────────────────────────┘
                                │ depends on
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│  src/integration/wirespec  (JVM-only — runtime artifact)         │
│                                                                   │
│  Wirespec.kt    Generator + GeneratorField* (already present)    │
│  Wirespec.java  Generator + GeneratorField* (NEW — added here)   │
└──────────────────────────────────────────────────────────────────┘

For Scala: no shipped runtime. The adapter loads
community.flock.wirespec.scala.Wirespec$Generator at first call, from
whatever the user's --emit-shared placed on the classpath.
```

Why this shape:

- `Wirespec.java` is the natural home for the Java `Generator` declarations:
  the published `wirespec-jvm` artifact is already what Java consumers depend
  on, and the IR converter already emits identical types under
  `--emit-shared`. Updating the static file makes the runtime artifact
  structurally equivalent to emit-shared output.
- The Scala dynamic-proxy approach avoids creating a Scala-plugin Gradle
  subproject *and* avoids submoduling the kotest module itself. The kotest
  JAR stays pure-Kotlin/JVM. The cost is that classpath misconfiguration
  surfaces at first call rather than at compile time — acceptable for a
  test-only integration where the adapter and the user's emitted code are
  always built and run together.
- All three adapters share the existing commonMain `KotestGenerator` /
  `KotestField*` machinery. The default arb catalog, regex generator, `@Seed`
  two-pass capture/replay, and `@Generator(...)` dispatch are written exactly
  once.

## Public API

Symmetric trio of factories, all returning their language's
`Wirespec.Generator`:

```kotlin
// Kotlin-emitted code (renamed from kotestWirespecGenerator)
fun kotestWirespecKotlinGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): community.flock.wirespec.kotlin.Wirespec.Generator

// Java-emitted code (statically typed against the runtime Wirespec.java)
fun kotestWirespecJavaGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): community.flock.wirespec.java.Wirespec.Generator

// Scala-emitted code (dynamic proxy; cast at the call site)
fun kotestWirespecScalaGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Any
```

The shared `KotestWirespecGeneratorBuilder` (with
`register(name) { Arb<String> }`) is reused across all three. Same DSL, same
default catalog (`email`, `firstName`, `uuid`, …), same `@Generator` /
`@Seed` semantics.

`kotestWirespecGenerator(...)` (the bare name) is **renamed** to
`kotestWirespecKotlinGenerator(...)`. This is a breaking change. The pre-1.0
status of the integration and the symmetry win make it worth it; existing
callers get a one-line edit per call site.

## Data flow

Each adapter implements `Wirespec.Generator.generate(path, field)` by:

1. Translating the language-shaped `Wirespec.GeneratorField*` into the
   commonMain `KotestField*` mirror.
2. Delegating to the shared `kotestGenerator(seed, block)` `KotestGenerator`.
3. Adapting the result back into the language's expected return type when it
   differs (e.g. `T?` → `Optional<T>`, Kotlin `List<T>` →
   `scala.collection.immutable.List[T]`).

### Java translations

| `Wirespec.GeneratorField*` (Java)                                       | `KotestField*` (commonMain)                            |
|--------------------------------------------------------------------------|--------------------------------------------------------|
| `GeneratorFieldString(Optional<String> regex, List<Map<String,Object>>)` | `KotestFieldString(regex.orElse(null), annotations)`   |
| `GeneratorFieldInteger(Optional<Long> min, Optional<Long> max, …)`       | `KotestFieldInteger(min.orElse(null), …)`              |
| `GeneratorFieldNumber(Optional<Double> min, …)`                          | `KotestFieldNumber(min.orElse(null), …)`               |
| `GeneratorFieldBoolean / Bytes`                                          | `KotestFieldBoolean / Bytes` — annotations only        |
| `GeneratorFieldEnum(values, annotations, java.lang.reflect.Type type)`   | `KotestFieldEnum(values, annotations, typeOf<Any>())`  |
| `GeneratorFieldUnion(...)`                                               | `KotestFieldUnion(...)` — same `KType` placeholder     |
| `GeneratorFieldArray<T>(Function<List<String>, T>)`                      | `KotestFieldArray<T>(generate = { p -> fn.apply(p) })` |
| `GeneratorFieldNullable<T>(Function<List<String>, T>)`                   | `KotestFieldNullable<T>` — result wrapped in `Optional.ofNullable(...)` |
| `GeneratorFieldShape<T>(annotations, generate, Type type)`               | `KotestFieldShape<T>` — same `KType` placeholder       |
| `GeneratorFieldDict<V>(Function<List<String>, V>)`                       | `KotestFieldDict<V>` — already returns Kotlin `Map<String,V>`, JVM-compatible with `java.util.Map` |

`KotestField*` carries a `kotlin.reflect.KType` for `Enum` / `Union` /
`Shape`, but the kotest implementation never reads it. Java's
`java.lang.reflect.Type` is dropped on entry; a synthetic `typeOf<Any>()`
fills the slot.

The crucial wrinkle is `GeneratorFieldNullable<T>`: it implements
`GeneratorField<Optional<T>>` in Java but `GeneratorField<T?>` in Kotlin.
The adapter wraps the inner result in `Optional.ofNullable(...)` before
returning. All other Java return types match Kotlin's at the JVM bytecode
level (`List`, `Map`, primitives, records).

### Scala translations (reflective)

The adapter is a `java.lang.reflect.InvocationHandler` that satisfies
`community.flock.wirespec.scala.Wirespec$Generator`, loaded at first call:

```kotlin
fun kotestWirespecScalaGenerator(seed: Long = 0L, block: ... = {}): Any {
    val inner = kotestGenerator(seed, block)
    val cl = Thread.currentThread().contextClassLoader
    val generatorIface = cl.loadClass("community.flock.wirespec.scala.Wirespec\$Generator")
    return Proxy.newProxyInstance(cl, arrayOf(generatorIface)) { _, method, args ->
        // method.name == "generate"
        val path  = ScalaInterop.scalaListToKotlin(args[0])
        val field = ScalaInterop.fieldToKotest(args[1])
        val result = inner.generate(path, field)
        ScalaInterop.adaptResultForScala(result, args[1])
    }
}
```

| Scala (runtime, reflectively)                | Kotlin (commonMain)   | Mechanism                                                                                  |
|----------------------------------------------|-----------------------|--------------------------------------------------------------------------------------------|
| `scala.collection.immutable.List<String>`    | `List<String>`        | `scala.jdk.javaapi.CollectionConverters$.MODULE$.asJava(list)` then iterate                |
| `scala.Option<T>`                            | `T?`                  | `option.isEmpty()` / `option.get()`; constructing: `scala.Option$.MODULE$.apply(value)`    |
| `scala.Function1<List<String>, T>`           | `(List<String>) -> T` | wrap as a `Function1` proxy whose `apply(arg)` delegates to the Kotlin lambda              |
| `scala.collection.immutable.Map[String, V]`  | `Map<String, V>`      | `CollectionConverters$.MODULE$.asScala(...)` for results; reverse for parameters           |
| `scala.reflect.ClassTag<?>`                  | (placeholder)         | unread by kotest impl; pass `ClassTag$.MODULE$.Any()` reflectively when wrapping back      |
| `Wirespec$GeneratorFieldString` (case class) | `KotestFieldString`   | `getClass().getSimpleName()` discriminates; case-class accessors `regex()`, `annotations()`|
| `Wirespec$GeneratorFieldNullable<T>`         | `KotestFieldNullable<T>` — result re-wrapped in `scala.Option`                             |
| `Wirespec$GeneratorFieldArray<T>`            | `KotestFieldArray<T>` — result re-wrapped in `scala.collection.immutable.List`             |

All Scala interop lives in a single `ScalaInterop.kt` file. Reflective
lookups (`Class.forName`, `Method`, `Field`) are cached in `lazy` properties
so the per-call cost is method-dispatch only, not lookup.

## Error handling

| Failure                                                             | Behavior                                                                                                                                  |
|---------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| Unknown `@Generator("foo")` name                                    | Already handled in commonMain: `error("Unknown @Generator name: 'foo' — register it via …")`. Same wording across all three adapters.     |
| Java `GeneratorFieldEnum` whose `Type` is malformed                 | Adapter ignores `Type` (kotest impl doesn't read it). No error.                                                                           |
| Java `GeneratorFieldNullable.generate` returns `null`               | Wrapped as `Optional.empty()` — matches `Optional.ofNullable(null)`. Already covered.                                                     |
| Scala `Wirespec$Generator` missing from classpath                   | First call: `ClassNotFoundException`, rethrown as `IllegalStateException` with hint: *"Scala-emitted Wirespec.scala not found on classpath — make sure your codegen runs with --emit-shared and the generated source set is on test compile/runtime."* |
| Scala field record class shape doesn't match (older Wirespec.scala) | `NoSuchMethodException` on accessor lookup, rethrown with the missing accessor name and the expected shape.                               |

## File-by-file change list

### Modified

- `src/integration/wirespec/src/jvmMain/java/community/flock/wirespec/java/Wirespec.java`
  Add `interface Generator` + 11 `GeneratorField*` records, matching the IR
  emitter output in `JavaIrEmitterTest.sharedOutputTest` exactly. Purely
  additive; no Kotlin metadata bump.

- `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJvm.kt`
  → renamed to `KotestWirespecKotlinGenerator.kt`.
  Public function `kotestWirespecGenerator(...)` → renamed to
  `kotestWirespecKotlinGenerator(...)`. Internal class
  `WirespecGeneratorAdapter` → renamed to `WirespecKotlinGeneratorAdapter`.

- `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJvmTest.kt`
  → renamed to `KotestWirespecKotlinGeneratorJvmTest.kt`. Updates references.

- `src/integration/kotest/README.md`
  Examples updated to the new function name; new sections for Java and Scala
  usage (with the Scala caveat about `--emit-shared`).

### New

- `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecJavaGenerator.kt`
  Public `kotestWirespecJavaGenerator(...)` factory and internal
  `WirespecJavaGeneratorAdapter`.

- `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecScalaGenerator.kt`
  Public `kotestWirespecScalaGenerator(...)` factory and internal
  `WirespecScalaGeneratorAdapter` (a `java.lang.reflect.InvocationHandler`).

- `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/ScalaInterop.kt`
  Reflective Scala ↔ Kotlin conversions, all cached via `lazy`.

- `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecJavaGeneratorJvmTest.kt`
  Constructs Java `Wirespec.GeneratorField*` records and verifies:
  dispatching every variant, the `Optional` round-trip, default arb catalog
  resolution, custom `register(...)` overrides, and seed determinism.

- `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecScalaGeneratorJvmTest.kt`
  Uses ByteBuddy (or hand-built reflective fakes) to fabricate a
  `community.flock.wirespec.scala.Wirespec$Generator` interface and matching
  field classes on the test classpath, then exercises the adapter through
  the dynamic proxy. Verifies the same surface as the Java test.

## Build sequence

Each step is one focused commit; bisecting a regression is trivial.

1. **Add `Generator` types to `Wirespec.java`** in `:src:integration:wirespec`.
   Verify `:src:integration:wirespec:build` is green and the Java emitter's
   `sharedOutputTest` still matches (the static and emitted shapes must stay
   structurally identical).

2. **Rename Kotlin adapter** — file + function + internal class — and update
   the test file. Verify `:src:integration:kotest:jvmTest` is green.

3. **Add Java adapter** plus its test. Verify
   `:src:integration:kotest:jvmTest` is green.

4. **Add Scala adapter** plus `ScalaInterop.kt` and its test. Verify
   `:src:integration:kotest:jvmTest` is green.

5. **Update `README.md`** with all three usage sections (including the Scala
   `--emit-shared` caveat and the cast-at-call-site pattern).

## Open questions

None. All design decisions resolved during brainstorming.
