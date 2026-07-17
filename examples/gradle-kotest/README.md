# Example: Kotest integration on a Spring Boot app

A small Spring Boot service — HTTP endpoints **and** an async Kafka channel — whose whole
contract lives in one Wirespec file, tested end-to-end with **Kotest**, the **Spring Kotest
extension**, and the **Wirespec Kotest scenario DSL**.

```
src/main/wirespec/campaign.ws     ── the single source of truth (types, endpoints, a channel)
        │  Gradle wirespec plugin (Kotlin IR emitter + two IR extensions)
        ▼
build/generated/.../generated/
   ├── model/ endpoint/ channel/ client/   ← models, Spring `@RestController` handler interfaces
   └── kotest/                              ← the typed scenario DSL (KotestDslExtension)
```

## What it demonstrates

- **One contract, two surfaces.** `campaign.ws` defines products/campaigns REST endpoints and a
  `CampaignEvents` channel. The `SpringMappingAnnotationsExtension` adds Spring routing
  annotations to the generated handler interfaces; the `KotestDslExtension` emits a typed test
  DSL next to the models. Both are enabled via the Gradle plugin's `extensionClasses`
  (see [`build.gradle.kts`](build.gradle.kts)).
- **Generated handler interfaces → controllers.** `ProductController` / `CampaignController`
  implement the generated `*.Handler` interfaces; `@EnableWirespecController` wires them up.
- **Generated channel interfaces → Kafka.** `CampaignEventPublisher` implements the generated
  `CampaignEvents.Sender`, `CampaignEventConsumer` implements `CampaignEvents.Listener`.
- **End-to-end tests through the DSL.** Specs never hand-build a request or a JSON body:

  ```kotlin
  // endpoint: build a request, send it over real HTTP, narrow the validated response
  val response = CreateProduct.generate.request {
      body { sku = Arb.constant("SKU-001"); name = Arb.constant("Wireless mouse"); price = Arb.constant(29.95) }
  }.call()
  response.shouldBeInstanceOf<CreateProduct.Response201>()

  // channel: publish a generated message; assert on it with a plain Kafka consumer
  CampaignEvents.generate.message { eventType = Arb.constant(CampaignEventType.ENDED) }.send(CAMPAIGN_EVENTS_TOPIC)
  ```

  The DSL drives the **send** direction only; asserting on what the app published to Kafka is done
  with a standard Kotest consumer (see `awaitCampaignEvent` in `CampaignChannelScenarioTest`).

## Layout

| Path | Role |
|---|---|
| `src/main/wirespec/campaign.ws` | The app's own contract (its REST surface + channel) |
| `src/main/wirespec/inventory.ws` | A downstream service the app *calls* (client only) |
| `controller/` | Implement the generated `*.Handler` interfaces (incl. `AvailabilityController`) |
| `downstream/` | `InventoryClient` + HTTP transportation driving the generated `GetStock` client |
| `service/`, `repository/` | In-memory domain logic |
| `kafka/` | Publisher/consumer implementing the generated channel `Sender`/`Listener` |
| `CampaignEndpointScenarioTest` | HTTP scenarios |
| `CampaignChannelScenarioTest` | Kafka messaging scenarios (`@EmbeddedKafka`) |
| `ProductAvailabilityMockTest` | Mocks the downstream inventory service with `.mock { req -> … }` |
| `ProjectConfig` | Registers every extension once for the whole suite, plus `KafkaChannelTransport` (producer-only `ChannelTransport` backing `send()`) and `WireMockMockServer` (the `MockServer` the `.mock` DSL drives) |

## How the tests wire the transport

The scenario DSL resolves its transport from an ambient context installed by three framework-agnostic
Wirespec extensions — `WirespecEndpointExtension` (backs `call()`), `WirespecChannelExtension` (backs
`send()`), and `WirespecMockExtension` (backs `.mock { }`). None depends on Spring: each takes
`suspend` factories for its transport/server and `Wirespec.Serialization`. All are registered once —
alongside Kotest's `SpringExtension` — in a single `ProjectConfig` (pointed at with the
`kotest.framework.config.fqn` system property in `build.gradle.kts`, so it can live in this package
rather than the default `io.kotest.provided.ProjectConfig`). Specs then carry no extension wiring at
all (just `@SpringBootTest` / `@EmbeddedKafka` to declare their context). The factories supply the
Spring wiring, reading the server port, the `Wirespec.Serialization` bean, and the Kafka bootstrap
servers from the test context via `testContextManager()`:

```kotlin
class ProjectConfig : AbstractProjectConfig() {
    override val extensions = listOf(
        SpringExtension(),                       // loads each spec's @SpringBootTest context (listed first)
        WirespecEndpointExtension(
            serialization = { serialization() },
            transportation = { HttpTransportation("http://localhost:${property("local.server.port")}") },
        ),
        WirespecChannelExtension(
            serialization = { serialization() },
            transportation = { KafkaChannelTransport(property("spring.kafka.bootstrap-servers")) },
            defaultTopic = CAMPAIGN_EVENTS_TOPIC,
        ),
        WirespecMockExtension(
            server = inventoryMockServer,        // suite-wide, started eagerly (see the mocking section)
            serialization = { serialization() },
        ),
    )
}
```

`SpringExtension` is listed first so it wraps the others and loads the context they read. The channel
extension builds one transport **per spec** (so the same registered instance serves the endpoint and
`@EmbeddedKafka` channel specs correctly); the mock extension shares one suite-wide server and only
resets its stubs between tests. The endpoint, channel and mock ambients compose, so a scenario can
drive REST, assert on an emitted event, and get a mocked downstream reply all at once.

## Mocking a downstream service

The app doesn't only *serve* its contract — it also *calls out*. A second contract,
[`inventory.ws`](src/main/wirespec/inventory.ws), describes a downstream inventory service; the app's
`GET /products/{id}/availability` (`AvailabilityController`) looks up a product and then calls that
service through the Wirespec-generated `GetStock` **client** (`InventoryClient`, pointed at
`inventory.base-url`). `CampaignEndpointScenarioTest`'s sibling `ProductAvailabilityMockTest` stubs
the downstream so the whole path runs without a real inventory service.

The response side of the DSL is the mirror image of `call()`. Where `Gen<Request>.call()` draws a
request and **sends** it, `Gen<Response<*>>.mock { req -> … }` draws a response and **stubs** it on a
mock server for every incoming request the typed predicate accepts:

```kotlin
// stub GET /stock/{sku}: only requests for SKU-001 get this canned 200
GetStock.generate.response200 {
    body = StockLevel.generate { sku = Arb.constant("SKU-001"); available = Arb.constant(7L); warehouse = Arb.constant("EU-WEST") }
}.mock { req -> req.path.sku == "SKU-001" }

// drive the app; its outbound GetStock call is answered by the stub above
val availability = GetProductAvailability.generate.request { path { id = Arb.constant(productId) } }.call()
availability.shouldBeInstanceOf<GetProductAvailability.Response200>()
availability.body.available shouldBe 7L
```

`req` is the fully typed `GetStock.Request`, so `req.path.sku` / `req.queries` / `req.body` read
through the generated names. The stub matches on the endpoint's method and path template first, then
on the predicate — so two stubs with different predicates route each SKU to its own response.

The mock server is resolved from an ambient context installed by `WirespecMockExtension` — the
response-side counterpart to `WirespecEndpointExtension`. The extension consumes a framework-neutral
[`MockServer`](../../src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/WirespecMockContext.kt);
`WireMockMockServer` (in [`ProjectConfig.kt`](src/test/kotlin/community/flock/wirespec/examples/kotest/ProjectConfig.kt))
is the WireMock-backed implementation, the mock analogue of `KafkaChannelTransport` — it reuses the
wirespec WireMock integration (`community.flock.wirespec.integration:wiremock`) `requestBuilder` /
`responseBuilder` to turn each stub's method/path and serialized response into a WireMock mapping,
adding only the typed-predicate matcher on top. Like the endpoint
and channel extensions it is registered once for the whole suite in `ProjectConfig`, against a
suite-wide `inventoryMockServer` started eagerly so its `baseUrl` is known before any context boots
(the caller-owned form: stubs are reset between tests, the server is left open):

```kotlin
// ProjectConfig.kt — one registration for the whole suite
val inventoryMockServer: WireMockMockServer = WireMockMockServer.start()   // dynamic port

class ProjectConfig : AbstractProjectConfig() {
    override val extensions = listOf(
        SpringExtension(),
        // …endpoint + channel extensions…
        WirespecMockExtension(server = inventoryMockServer, serialization = { serialization() }),
    )
}
```

Only the spec that actually mocks the downstream then wires the app's `inventory.base-url` at that
server, via a `@DynamicPropertySource` that runs before its Spring context boots:

```kotlin
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ProductAvailabilityMockTest : FunSpec({
    test("…") { /* .mock { } stubs, .call() drives the app */ }
}) {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun inventoryProperties(registry: DynamicPropertyRegistry) {
            registry.add("inventory.base-url") { inventoryMockServer.baseUrl }
        }
    }
}
```

## Running

```shell
./gradlew build          # generate, compile, run all tests
./gradlew test           # tests only
./gradlew bootRun        # start the service (needs a Kafka broker at localhost:9092 to publish)
```

No Docker or external broker is needed to run the tests:

- The **endpoint** scenarios use a real embedded server on a random port and don't touch Kafka
  (event publishing is fire-and-forget).
- The **channel** scenarios spin up an in-JVM Kafka broker with `@EmbeddedKafka`; the app and the
  test's `KafkaChannelTransport` both point at it.

## Notes

- The `KotestDslExtension` emits the DSL into the **main** source set alongside the models, so the
  main compilation depends on `kotest-property` and the Wirespec Kotest runtime (see the
  `implementation` entries in `build.gradle.kts`).
