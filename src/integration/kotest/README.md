# Wirespec ↔ Kotest integration

`Wirespec.Generator` implementation backed by [Kotest][kotest] [`Arb`][arb]s and
the [Extra Arbs][extras]. Drives the IR-emitted `*Generator.generate(...)`
factories with deterministic, configurable test data — drop-in replacement for
hand-written `SeededGenerator` classes.

[kotest]: https://kotest.io
[arb]: https://kotest.io/docs/proptest/property-test-generators.html
[extras]: https://kotest.io/docs/proptest/property-test-extra-arbs.html

## Targets

The integration is multiplatform (JVM + JS/IR). All defaults live in
`commonMain` since `kotest-property-arbs` 3.0.0 ships an IR-compatible
multiplatform artifact under the new `io.kotest` group.

| Capability                                                      | JVM | JS  |
| --------------------------------------------------------------- | :-: | :-: |
| `Wirespec.Generator` impl, `@Seed`, DSL                         | ✅  | ✅  |
| Regex-validated `String` fields                                 | ✅  | ✅  |
| Default `email`, `ipAddress`, `uuid`                            | ✅  | ✅  |
| `kotest-property-arbs` extras (`firstName`, `color`, …)         | ✅  | ✅  |

Regex generation uses [`community.flock.kotlinx.rgxgen`][rgxgen] (multiplatform),
not `Arb.stringPattern` (which is JVM-only in kotest 6.x). The default `uuid`
arb is an in-house portable RFC 4122 v4 generator (kotest core's `Arb.uuid()`
wraps `java.util.UUID` and is JVM-only).

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

`kotest-property-arbs` is pulled in transitively — the integration depends on
`io.kotest:kotest-property-arbs:3.0.0` directly, no separate declaration needed.

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

| Name        | Arb source                      | JVM | JS  | Notes                          |
| ----------- | ------------------------------- | :-: | :-: | ------------------------------ |
| `email`     | `Arb.email()`                   | ✅  | ✅  | `local@domain` style strings   |
| `ipAddress` | `Arb.ipAddressV4()`             | ✅  | ✅  | Dotted-quad IPv4               |
| `uuid`      | in-house RFC 4122 v4            | ✅  | ✅  | Portable, seed-respecting      |
| `firstName` | `Arb.firstName()`               | ✅  | ✅  | Extra Arbs                     |
| `lastName`  | `Arb.lastName()`                | ✅  | ✅  | Extra Arbs                     |
| `fullName`  | `Arb.name()`                    | ✅  | ✅  | First + last; alias of `name`  |
| `name`      | `Arb.name()`                    | ✅  | ✅  | Same as `fullName`             |
| `username`  | `Arb.usernames()`               | ✅  | ✅  | Extra Arbs                     |
| `domain`    | `Arb.domain()`                  | ✅  | ✅  | e.g. `www.wibble.co.uk`        |
| `color`     | `Arb.color()`                   | ✅  | ✅  | Named colors                   |

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
    register("orderId") { Arb.int(0..999_999).map { "ORD-%06d".format(it) } }
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
