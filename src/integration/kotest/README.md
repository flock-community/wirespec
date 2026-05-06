# Wirespec ↔ Kotest integration

`Wirespec.Generator` implementation backed by [Kotest][kotest] [`Arb`][arb]s and
the [Extra Arbs][extras]. Drives the IR-emitted `*Generator.generate(...)`
factories with deterministic, configurable test data — drop-in replacement for
hand-written `SeededGenerator` classes.

[kotest]: https://kotest.io
[arb]: https://kotest.io/docs/proptest/property-test-generators.html
[extras]: https://kotest.io/docs/proptest/property-test-extra-arbs.html

## Dependency

```kotlin
testImplementation("community.flock.wirespec.integration:kotest-jvm:<version>")
testImplementation("io.kotest:kotest-property:<version>")
testImplementation("io.kotest.extensions:kotest-property-arbs:2.1.2")
```

The integration is JVM-only (the emitted `Wirespec.Generator` interface uses
`kotlin.reflect.KType`).

## Basic usage

```kotlin
import community.flock.wirespec.integration.kotest.kotestWirespecGenerator

val gen = kotestWirespecGenerator(seed = 1L)
val member: Member = MemberGenerator.generate(gen, emptyList())
val project: Project = ProjectGenerator.generate(gen, emptyList())
```

Each `kotestWirespecGenerator(seed = …)` is deterministic: same seed → same
output for the same generated type.

## Default `@Generator(...)` registrations

Annotate fields in your `.ws` file with `@Generator("name")` to route them to
named arbs. The integration ships these defaults (all matched
case-insensitively):

| Name        | Arb source                      | Notes                          |
| ----------- | ------------------------------- | ------------------------------ |
| `email`     | `Arb.email()`                   | `local@domain` style strings   |
| `uuid`      | `Arb.uuid()`                    | Stringified UUIDs              |
| `ipAddress` | `Arb.ipAddressV4()`             | Dotted-quad IPv4               |
| `firstName` | `Arb.firstName()`               | Extra Arbs                     |
| `lastName`  | `Arb.lastName()`                | Extra Arbs                     |
| `fullName`  | `Arb.name()`                    | First + last; alias of `name`  |
| `name`      | `Arb.name()`                    | Same as `fullName`             |
| `username`  | `Arb.usernames()`               | Extra Arbs                     |
| `domain`    | `Arb.domain()`                  | e.g. `www.wibble.co.uk`        |
| `color`     | `Arb.color()`                   | Named colors                   |

Wirespec definition:

```wirespec
type Member {
    @Generator("email")    email: String,
    @Generator("fullName") name: String
}
```

## Custom registrations

Register your own `Arb<String>`s (or override the defaults) via the builder
block:

```kotlin
val gen = kotestWirespecGenerator(seed = 1L) {
    register("orderId") { Arb.uuid().map { "ORD-$it" } }
    register("email")   { Arb.constant("test@example.com") }   // overrides default
}
```

Names are matched **case-insensitively**: `@Generator("orderId")` and
`@Generator("ORDERID")` resolve to the same registration.

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

## RandomSource overload

If you need to share a `RandomSource` with other Kotest property tests:

```kotlin
import io.kotest.property.RandomSource

val rs = RandomSource.seeded(42L)
val gen = kotestWirespecGenerator(rs) { /* ... */ }
```
