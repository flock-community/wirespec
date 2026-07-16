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

  // channel: assert on what the app published to Kafka
  CampaignEvents.generate.listen {
      expecting { event -> event.eventType shouldBe CampaignEventType.CREATED }
  }
  ```

## Layout

| Path | Role |
|---|---|
| `src/main/wirespec/campaign.ws` | The contract |
| `controller/` | Implement the generated `*.Handler` interfaces |
| `service/`, `repository/` | In-memory domain logic |
| `kafka/` | Publisher/consumer implementing the generated channel `Sender`/`Listener` |
| `CampaignEndpointScenarioTest` | HTTP scenarios |
| `CampaignChannelScenarioTest` | Kafka messaging scenarios (`@EmbeddedKafka`) |
| `support/KafkaChannelTransport` | `ChannelTransport` backing `send()` / `expecting()` over Kafka |

## How the tests wire the transport

The scenario DSL resolves its transport from an ambient context installed by two framework-agnostic
Wirespec extensions — `WirespecEndpointExtension` (backs `call()`) and `WirespecChannelExtension`
(backs `send()` / `expecting()`). Neither depends on Spring: each takes `suspend` factories for the
transport(ation) and `Wirespec.Serialization`. Each spec registers them in its body and supplies the
Spring wiring in those factories — reading the server port, the `Wirespec.Serialization` bean, and
the Kafka bootstrap servers from the test context via `testContextManager()`:

```kotlin
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ApplyExtension(SpringExtension::class)
class CampaignEndpointScenarioTest : FunSpec({
    extension(
        WirespecEndpointExtension(
            serialization = { testContextManager().testContext.applicationContext.getBean(Wirespec.Serialization::class.java) },
            transportation = {
                val ctx = testContextManager().testContext.applicationContext
                HttpTransportation("http://localhost:${ctx.environment.getProperty("local.server.port")}")
            },
        ),
    )
    test("…") {
        val response = CreateProduct.generate.request { … }.call()
        // …
    }
})
```

The Kotest `SpringExtension` — applied via `@ApplyExtension` — loads the Spring context the factories
read from. The channel spec registers both extensions; they compose into one ambient, so the same
spec can drive REST and then assert on the emitted event.

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
