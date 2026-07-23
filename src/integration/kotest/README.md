# Wirespec ↔ Kotest integration

Two pieces, usable together or independently:

1. A **`Wirespec.Generator` implementation** backed by [Kotest][kotest]
   [`Arb`][arb]s, driving the IR-emitted `*Generator.generate(...)` factories
   with deterministic, configurable test data.
2. A **scenario DSL** emitted alongside your models by `KotestDslExtension`, so
   endpoints and channels can be exercised as
   `GetTodos.generate.request { … }.call()`.

[kotest]: https://kotest.io
[arb]: https://kotest.io/docs/proptest/property-test-generators.html

## Targets

Only the `jvm()` target is declared, against Kotlin-emitted code, but most of
the module lives in `commonMain` and uses no JVM APIs:

- **`commonMain`** — the generator algorithm and `KotestField` mirror types, the
  `KotestDslExtension` emitter (operates on the multiplatform compiler AST/IR),
  and the framework-neutral `WirespecTestContext` / `WirespecChannelContext` /
  `WirespecAmbient` types. These reference only the `Wirespec.*` interfaces
  (themselves `commonMain` in `:src:integration:wirespec`), `kotest-property`,
  and `kotlin.coroutines`.
- **`jvmMain`** — everything that reflects over generated classes at runtime
  (`ArbReceiver`, `EndpointReflection`, `CallExecutor`, the `*CallBuilder`
  terminals, `JvmRefinedWrapper`) or needs a JVM primitive (`WirespecRequestScope`
  uses `ThreadLocal`). These are anchored to the JVM by `kotlin-reflect` and
  `java.*`, not by choice of target.

Regex generation uses [`community.flock.kotlinx.rgxgen`][rgxgen] (multiplatform),
not `Arb.stringPattern` (which is JVM-only in kotest 6.x).

[rgxgen]: https://github.com/flock-community/kotlin-rgxgen

## Dependency

```kotlin
testImplementation("community.flock.wirespec.integration:kotest-jvm:<version>")
testImplementation("io.kotest:kotest-property:<version>")
```

## Part 1 — the generator

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

### Overrides

Pin or customize generated values via the builder block. Three forms, in
precedence order (`@Seed` always wins, see below):

```kotlin
val gen = kotestWirespecKotlinGenerator(seed = 1L) {
    // 1. By path — exact segments; `*` matches any single segment
    //    (e.g. an array index). Most specific pattern wins.
    registerPath("users", "*", "email") { Arb.email() }
    registerPath("users", "0", "id", value = "FIXED-ID")

    // 2. By parent type + field — compile-checked property reference.
    registerField(Member::id) { Arb.uuid().map(java.util.UUID::toString) }
    registerField(Member::age, value = 42L)

    // 3. By parent type name + field name (stringly, no reflection).
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

### `@Seed` semantics

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

## Part 2 — the scenario DSL

`KotestDslExtension` is an `IrExtension`: register it on a Kotlin IR emitter and
it emits one `<Endpoint>Dsl.kt` / `<Channel>Dsl.kt` per operation into
`<packageName>.kotest`, alongside the models the base emitter produces.

```kotlin
KotlinIrEmitter(pkg, EmitShared(false)).applyExtensions(listOf(KotestDslExtension(pkg)))
```

Each generated file hangs the entry points off a `generate` extension property
on the endpoint/channel object. Each returns a `Gen<…>`; sending is chained off it
with `call()` / `send()`, which draw one value and return what went over the wire:

```kotlin
// Build a request Gen, send one with call(), narrow the returned variant.
val response = PutTodo.generate.request {
    path { id("42") }
    body { name("milk") }
}.call()
response.shouldBeInstanceOf<PutTodo.Response200>()

// Every builder entry point returns a Gen<…>; draw() one to inspect it (or feed it to checkAll).
val request: Gen<PutTodo.Request> = PutTodo.generate.request { path { id("42") } }
val canned: PutTodo.Response200 = PutTodo.generate.response200().draw()

// Channels: publish by chaining send() on the message Gen. send() takes an optional
// destination topic (and key); omit them to fall back to the channel object's simple name.
CampaignEvents.generate.message { eventType(CampaignEventType.ENDED) }.send("campaign-events")

// Channels: consume via the listen scope.
CampaignEvents.generate.listen { expecting { event -> event.eventType shouldBe CampaignEventType.CREATED } }

// Standalone types: `<Type>.generate { … }` reads through the type name like the
// endpoint/channel entry points and returns a `Gen<…>`, pinning only the fields you set.
val gen: Gen<Campaign> = Campaign.generate { name("Summer sale") }
val one: Campaign = gen.draw()
```

Every scope slot has both an assignable `var` form (`path = { … }`) and a
same-named function form (`path { … }`). Un-overridden fields keep the value the
generator drew, so a scenario only spells out the fields it actually cares about.

Field slots follow the same pair. Each is a `Gen<…>?` property plus a same-named
single-arg function that pins a constant, so a fixed value needs no `Arb` wrapper:

```kotlin
body {
    name("Autumn promo")               // == name = Arb.constant("Autumn promo")
    discountPercentage(20L)
    productIds = Arb.list(productIdArb) // the property form still takes any Gen/Arb
}
```

`<Type>.generate` is an extension on the type's companion object; the extension injects an
empty `companion object` into each generated record so there is a receiver to hang it
on (the model stays dependency-free — the `generate` logic lives in the kotest package). The
per-field builder behind every entry point is the single shared `<Type>Builder`:
endpoint request bodies, channel payloads and `<Type>.generate` all reference the same
builder for a given record, rather than each emitting its own copy. Nested record
fields expose a `<field>Block { … }` sub-block that opens the nested type's builder, so
overrides compose to any depth without duplicating builders.

### Wiring the transport

The DSL resolves its transport and per-test `RandomSource` from an ambient
context installed by two extensions — one per direction. Register whichever the
spec drives (or both, they compose into one ambient sharing a `RandomSource`):

```kotlin
class MySpec : FunSpec({
    extension(WirespecEndpointExtension(WirespecEndpointContext(transportation, serialization)))
    extension(WirespecChannelExtension(WirespecChannelContext(transport, serialization)))
})
```

`WirespecEndpointExtension` backs `call()`, `WirespecChannelExtension` backs
`send()` / `expecting()`; supply whichever the spec needs. A call into a missing
context fails with a message naming the extension to add. Responses are
validated against the contract — an
undeclared status or a body that won't deserialize into the matched variant
fails the test with the seed printed for reproduction.

For endpoint specs against a running server, the module ships a ready
`Wirespec.Transportation` — `community.flock.wirespec.integration.jvm.transport.HttpTransportation`
(JDK `HttpClient`) — so you can wire `WirespecEndpointContext(HttpTransportation("http://localhost:$port"), serialization)`
without hand-rolling one.

For a spec whose single transport applies per-block identity (auth, headers),
hold the active config in a `WirespecRequestScope<C>` and have the transport read
`scope.current()` on each request.
