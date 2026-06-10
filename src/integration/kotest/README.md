# Wirespec ↔ Kotest integration

`Wirespec.Generator` implementation backed by [Kotest][kotest] [`Arb`][arb]s.
Drives the IR-emitted `*Generator.generate(...)` factories with deterministic,
configurable test data — drop-in replacement for hand-written
`SeededGenerator` classes.

[kotest]: https://kotest.io
[arb]: https://kotest.io/docs/proptest/property-test-generators.html

## Targets

The integration is multiplatform (JVM + JS/IR).

| Capability                                       | JVM | JS  |
| ------------------------------------------------ | :-: | :-: |
| `Wirespec.Generator` impl, `@Seed`               | ✅  | ✅  |
| Regex-validated `String` fields                  | ✅  | ✅  |
| Path overrides (`registerPath`)                  | ✅  | ✅ (constant values) |
| Field overrides (`registerField`, by parent type)| ✅  | ❌  |
| Refined auto-wrap on overrides                   | ✅  | ❌  |

Regex generation uses [`community.flock.kotlinx.rgxgen`][rgxgen] (multiplatform),
not `Arb.stringPattern` (which is JVM-only in kotest 6.x).

[rgxgen]: https://github.com/flock-community/kotlin-rgxgen

## Dependency

```kotlin
// JVM
testImplementation("community.flock.wirespec.integration:kotest-jvm:<version>")
testImplementation("io.kotest:kotest-property:<version>")

// JS / Kotlin Multiplatform
testImplementation("community.flock.wirespec.integration:kotest-js:<version>")
testImplementation("io.kotest:kotest-property:<version>")
```

## Basic usage — Kotlin-emitted code

```kotlin
import community.flock.wirespec.integration.kotest.kotestWirespecKotlinGenerator

val gen = kotestWirespecKotlinGenerator(seed = 1L)
val member: Member = MemberGenerator.generate(gen, emptyList())
val project: Project = ProjectGenerator.generate(gen, emptyList())
```

Each `kotestWirespecKotlinGenerator(seed = …)` is deterministic: same seed →
same output for the same generated type. Nullable fields draw `null` for ~20%
of paths (also deterministic per seed + path), so null branches get exercised.

The generator keeps per-call traversal state and is **not thread-safe** —
instances are cheap, create one per test rather than sharing across
concurrently running tests.

## Overrides

Pin or customize generated values via the builder block. Three forms, in
precedence order (`@Seed` always wins, see below):

```kotlin
val gen = kotestWirespecKotlinGenerator(seed = 1L) {
    // 1. By path — exact segments; `*` matches any single segment
    //    (e.g. an array index). Most specific pattern wins.
    registerPath("users", "*", "email") { Arb.email() }
    registerPath("users", "0", "id", value = "FIXED-ID")

    // 2. By parent type + field — compile-checked property reference (JVM).
    registerField(Member::id) { Arb.uuid().map(java.util.UUID::toString) }
    registerField(Member::age, value = 42L)

    // 3. By parent type name + field name (multiplatform, stringly).
    registerFieldByTypeName("com.example.Member", "email", value = "a@b.com")
}
```

- Factories accept any Kotest `Gen` (`Arb` or `Exhaustive`).
- In the `value = …` form the argument **must be passed by name**:
  `registerPath("users", "id", "FIXED")` would register the three-segment
  path `users/id/FIXED` because the vararg swallows positional strings.
- Registering the same path pattern or field key twice fails fast; two
  equally-specific patterns matching the same path fail at lookup with an
  "Ambiguous" error.

### Refined fields auto-wrap

When an override fires on a field whose type is a Wirespec `Refined` wrapper
(single-arg constructor), provide the **inner primitive** — the integration
wraps it for you:

```kotlin
// Member.id is a refined `MemberId(value: String)`
registerField(Member::id) { Arb.constant("m-1") }   // becomes MemberId("m-1")
```

A type mismatch (e.g. an `Arb<Long>` for a `String`-backed wrapper) raises an
error naming the path, the expected inner type, and the actual value type.
This works for Kotlin-, Java- and Scala-emitted code: all three adapters
propagate the field's target class.

## Java-emitted code

The Java emitter produces `*Generator.java` factories that take a
`community.flock.wirespec.java.Wirespec.Generator`. Use the Java sibling
factory:

```kotlin
import community.flock.wirespec.integration.kotest.kotestWirespecJavaGenerator
import community.flock.wirespec.java.Wirespec
import com.example.generated.MemberGenerator

val gen: Wirespec.Generator = kotestWirespecJavaGenerator(seed = 1L) {
    registerPath("users", "*", "email") { Arb.email() }
}
val member = MemberGenerator.generate(gen, java.util.List.of())
```

Same DSL and `@Seed` semantics as the Kotlin sibling. The two differences are
JVM-flavoured:

- `GeneratorFieldString.regex`, `GeneratorFieldInteger.min`/`max`, etc. carry
  `java.util.Optional<X>` instead of Kotlin's `X?` — the adapter handles the
  `Optional.empty()` ↔ `null` translation.
- `GeneratorFieldNullable<T>` returns `java.util.Optional<T>` (Java semantics)
  rather than the bare `T?` the commonMain algorithm produces.

## Scala-emitted code

The Scala emitter produces `*Generator.scala` factories that take a
`community.flock.wirespec.scala.Wirespec.Generator`. Use the Scala sibling
factory and cast at the call site — the kotest module has zero compile-time
Scala dependency, so the factory's static return type is `Any`:

```kotlin
import community.flock.wirespec.integration.kotest.kotestWirespecScalaGenerator
import community.flock.wirespec.scala.Wirespec  // from your --emit-shared output
import com.example.generated.generator.MemberGenerator

val gen: Wirespec.Generator =
    kotestWirespecScalaGenerator(seed = 1L) {
        registerPath("users", "*", "id") { Arb.constant("FIXED") }
    } as Wirespec.Generator

val member = MemberGenerator.generate(gen, scala.collection.immutable.List.empty())
```

**Requirement:** the user's codegen MUST run with `--emit-shared` so the
generated `Wirespec.scala` (which declares `Wirespec.Generator`) lands on the
test classpath. If it's missing, the factory raises:

> *Scala-emitted Wirespec.scala not found on classpath. Run your codegen with
> --emit-shared and make sure the generated source set is on the test
> compile/runtime classpath.*

Internally the Scala adapter is a `java.lang.reflect.Proxy` that resolves
`Wirespec.Generator` reflectively at construction time. Reflective Scala ↔
Kotlin conversions live in `ScalaInterop.kt`.

## JS-emitted code

The JS facade accepts plain-object fields with a `kind` discriminator and an
optional builder callback for constant-value path overrides:

```ts
const gen = kotestWirespecGeneratorJs(1, (b) => {
    b.registerPath(["users", "*", "id"], "FIXED-ID")
})
const value = gen.generate(["path"], { kind: "string", regex: undefined, annotations: [] })
```

Arb-backed overrides are JVM-only — Kotest `Gen` factories are not
`@JsExport`-able.

## `@Seed` semantics

Wirespec's `@Seed` field annotation gets honored: if a `Shape` has a child
field annotated `@Seed`, the seed value is taken from the parent path. This
lets you regenerate the same record deterministically from just an ID:

```kotlin
val byId = ProjectGenerator.generate(gen, listOf("proj-42"))
//   ↑ project's @Seed-annotated `id` field is "proj-42"
```

For arrays of records, the integration runs a two-pass capture/replay: the
first pass auto-generates the seed; the second pass propagates it through
nested refined wrappers so each element has stable, reproducible identity.

`@Seed` takes precedence over any registered override at the same path.
