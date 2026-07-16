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
| `mock/WireMockMockServer` | WireMock-backed `MockServer` the `.mock` DSL drives |
| `ProjectConfig` | Registers every extension once for the whole suite, plus the `KafkaChannelTransport` (a producer-only `ChannelTransport` backing `send()` over Kafka) |

## How the tests wire the transport

The scenario DSL resolves its transport from an ambient context installed by two framework-agnostic
Wirespec extensions — `WirespecEndpointExtension` (backs `call()`) and `WirespecChannelExtension`
(backs `send()`). Neither depends on Spring: each takes `suspend` factories for the
transport(ation) and `Wirespec.Serialization`. Both are registered once — alongside Kotest's
`SpringExtension` — in a single `ProjectConfig` (pointed at with the `kotest.framework.config.fqn`
system property in `build.gradle.kts`, so it can live in this package rather than the default
`io.kotest.provided.ProjectConfig`). Specs then carry no extension wiring at all (just
`@SpringBootTest` / `@EmbeddedKafka` to declare their context). The factories supply the Spring
wiring, reading the server port, the `Wirespec.Serialization` bean, and the Kafka bootstrap servers
from the test context via `testContextManager()`:

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
            transport = { KafkaChannelTransport(property("spring.kafka.bootstrap-servers")) },
            defaultTopic = CAMPAIGN_EVENTS_TOPIC,
            reset = { it.clear() },
        ),
    )
}
```

`SpringExtension` is listed first so it wraps the others and loads the context they read. The channel
extension builds one transport **per spec**, so the same registered instance serves both the endpoint
spec and the `@EmbeddedKafka` channel spec correctly, and closes each after its spec. The endpoint and
channel ambients compose, so a scenario can drive REST and then assert on the emitted event.

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
[`WireMockMockServer`](src/test/kotlin/community/flock/wirespec/examples/kotest/mock/WireMockMockServer.kt)
is the WireMock-backed implementation (the mock analogue of `KafkaChannelTransport`). It is started in
the spec's companion so its `baseUrl` can feed the app's `inventory.base-url` via
`@DynamicPropertySource` before the Spring context boots, then registered spec-locally so it composes
with the global endpoint extension:

```kotlin
class ProductAvailabilityMockTest : FunSpec() {
    init {
        // managed form: resets stubs between tests, stops the server after the spec
        extension(WirespecMockExtension(serialization = { mockSerialization() }, server = { mockServer }))
        test("…") { /* .mock { } stubs, .call() drives the app */ }
    }
    companion object {
        private val mockServer = WireMockMockServer.start()   // dynamic port

        @JvmStatic
        @DynamicPropertySource
        fun inventoryProperties(registry: DynamicPropertyRegistry) {
            registry.add("inventory.base-url") { mockServer.baseUrl }
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
