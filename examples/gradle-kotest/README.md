# gradle-kotest

A Spring Boot service whose **contract is written in Wirespec** — REST endpoints **and** a
Kafka channel — tested end-to-end with the generated **Kotest scenario DSL**.

The domain is campaigns and products: you manage `Product`s and `Campaign`s over HTTP, and
every campaign lifecycle change (`CREATED`, `ACTIVATED`) is emitted as a `CampaignEvent` onto
a Kafka channel.

## What this example shows

- A single Wirespec contract ([`src/main/wirespec/campaigns.ws`](src/main/wirespec/campaigns.ws))
  describing both the HTTP API and an async `channel CampaignEvents -> CampaignEvent`.
- The Wirespec Gradle plugin running the **`KotlinIrEmitter`** with two IR extensions:
  - `SpringMappingAnnotationsExtension` — annotates the generated endpoint handlers for Spring MVC.
  - `KotestDslExtension` — emits a typesafe Kotest scenario DSL (`<Op>.generate.call { … }`) next to the models.
- The generated `CampaignEvents.Sender` / `Listener` interfaces wired to Kafka with `spring-kafka`.
- Kotest specs that drive the real app through the generated DSL:
  - `CampaignEndpointScenarioTest` — `CreateProduct.generate.call { … }`, `GetCampaign.generate.call { … }`,
    etc. over real HTTP, validating the typed response variants against the contract.
  - `CampaignChannelScenarioTest` — `CampaignEvents.generate.call { … }` over an **embedded Kafka broker**,
    in both directions: the app emitting an event the DSL `expecting()`s, and the DSL `send`ing an
    event the app's `@KafkaListener` consumes.

## The scenario DSL

The `KotestDslExtension` generates one `<Operation>Dsl.kt` per endpoint and channel. Each operation
object gets a `generate` extension property grouping the scenario builders — `call { … }` to drive
the operation, `request { … }` / `response<NNN> { … }` to materialise typed values without sending:

```kotlin
// endpoint: build the request with kotest Arbs, validate the response variant
CreateProduct.generate.call {
    body { name = Arb.constant("Wireless Mouse") }      // unset fields are generated from the contract
    expecting<CreateProduct.Response201> { it.body.name shouldBe "Wireless Mouse" }
}

// materialise a random typed request/response instead of sending one
val request = CreateProduct.generate.request { body { name = Arb.constant("Wireless Mouse") } }
val response = CreateProduct.generate.response201()

// …or chain the built request straight into a send with the `call()` request extension
val sent = CreateProduct.generate.request { body { name = Arb.constant("Wireless Mouse") } }.call()

// channel: publish / consume typed payloads over Kafka
CampaignEvents.generate.call {
    expecting { event -> event.eventType shouldBe CampaignEventType.CREATED }
}
CampaignEvents.generate.call {
    send { eventType = Arb.constant(CampaignEventType.ENDED) }
}
```

The DSL runtime lives in the published `community.flock.wirespec.integration:kotest` artifact.
Specs are plain `FunSpec`s that register the ambient with
`extension(WirespecExtension(endpoint = …, channel = …))`, passing the transport contexts the
spec drives (see [`support/`](src/test/kotlin/community/flock/wirespec/examples/kotest/support)):
a JDK-`HttpClient` `Wirespec.Transportation` for endpoints and a Kafka producer/consumer
`ChannelTransport` for the channel. [`CampaignTestEnvironment`](src/test/kotlin/community/flock/wirespec/examples/kotest/support/CampaignTestEnvironment.kt)
boots an in-JVM Kafka broker (`spring-kafka-test`) and the real app once for the whole run — no
Docker required.

> Because the generated `*Dsl.kt` files import the kotest runtime and `io.kotest.property`, the
> kotest dependencies are on the main classpath here (not just test). The DSL is generated test
> scaffolding; if you prefer to keep kotest off your application's runtime classpath, generate it
> into a separate, test-only source set.

## Run it

```shell
# publish the Wirespec artifacts this example resolves from mavenLocal first (from the repo root)
./gradlew publishToMavenLocal --no-configuration-cache

# then, from this directory
./gradlew check
```

`./gradlew check` compiles the contract, the app, and the generated DSL, and runs the Kotest specs
against the embedded broker.
