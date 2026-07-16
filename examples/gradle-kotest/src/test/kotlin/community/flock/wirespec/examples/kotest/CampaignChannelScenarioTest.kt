package community.flock.wirespec.examples.kotest

import community.flock.wirespec.examples.kotest.generated.channel.CampaignEvents
import community.flock.wirespec.examples.kotest.generated.endpoint.ActivateCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.CreateCampaign
import community.flock.wirespec.examples.kotest.generated.kotest.call
import community.flock.wirespec.examples.kotest.generated.kotest.generate
import community.flock.wirespec.examples.kotest.generated.kotest.send
import community.flock.wirespec.examples.kotest.generated.model.CampaignEventType
import community.flock.wirespec.examples.kotest.generated.model.CampaignStatus
import community.flock.wirespec.examples.kotest.kafka.CAMPAIGN_EVENTS_TOPIC
import community.flock.wirespec.examples.kotest.support.KafkaChannelTransport
import community.flock.wirespec.integration.kotest.WirespecChannelExtension
import community.flock.wirespec.integration.kotest.WirespecEndpointExtension
import community.flock.wirespec.integration.kotest.transport.HttpTransportation
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.testContextManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka

/**
 * End-to-end messaging scenarios over an in-JVM Kafka broker (`@EmbeddedKafka`, no Docker),
 * driven through the Wirespec-generated channel DSL. The endpoint and channel extensions compose
 * into one ambient — an HTTP endpoint context (to drive the REST side that emits events) and a
 * Kafka channel context — so a single scenario can act on the app and then assert on what it
 * published, with no wiring in the spec body.
 *
 * Both wirespec extensions are framework-agnostic and registered in the spec body; this spec
 * supplies the Spring wiring in their `suspend` factories (resolving the context via
 * `testContextManager()`), so `@ApplyExtension` carries only kotest's own `SpringExtension`. They
 * compose into one ambient — an HTTP endpoint context (to drive the REST side that emits events)
 * and a Kafka channel context — so a scenario can act on the app and assert on what it published.
 *
 * `@EmbeddedKafka` starts the broker before the context loads and points the app's
 * `spring.kafka.bootstrap-servers` at it. The channel extension builds the [KafkaChannelTransport]
 * straight from the loaded context (no `@TestConfiguration` bean needed) and closes it after the
 * spec — so `generate.message { … }.send()` and `generate.listen { expecting { … } }` speak to the
 * exact topic the app produces to.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
    partitions = 1,
    topics = [CAMPAIGN_EVENTS_TOPIC],
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
)
@ApplyExtension(SpringExtension::class)
class CampaignChannelScenarioTest : FunSpec({

    // Both extensions are framework-agnostic; the Spring wiring lives here, in their suspend
    // factories, which read the loaded test context via `testContextManager()`.
    extension(
        WirespecEndpointExtension(
            serialization = {
                testContextManager().testContext.applicationContext.getBean(Wirespec.Serialization::class.java)
            },
            transportation = {
                val context = testContextManager().testContext.applicationContext
                val port = context.environment.getProperty("local.server.port") ?: error("Embedded server port not available")
                HttpTransportation("http://localhost:$port")
            },
        ),
    )
    extension(
        WirespecChannelExtension(
            serialization = {
                testContextManager().testContext.applicationContext.getBean(Wirespec.Serialization::class.java)
            },
            transport = {
                val context = testContextManager().testContext.applicationContext
                KafkaChannelTransport(context.environment.getProperty("spring.kafka.bootstrap-servers")!!)
            },
            // The transport now honours the resolved topic, so `listen` (which pins none) needs one:
            // supply it as the default for every message/listen on this channel.
            defaultTopic = CAMPAIGN_EVENTS_TOPIC,
            reset = { it.clear() },
        ),
    )

    test("creating a campaign publishes a CREATED event") {
        val created = CreateCampaign.generate.request {
            body {
                name = Arb.constant("Autumn promo")
                discountPercentage = Arb.constant(20L)
                productIds = Arb.constant(emptyList())
            }
        }.call()
        created.shouldBeInstanceOf<CreateCampaign.Response201>()

        CampaignEvents.generate.listen {
            expecting { event ->
                event.campaignId shouldBe created.body.id
                event.eventType shouldBe CampaignEventType.CREATED
                event.discountPercentage shouldBe 20L
            }
        }
    }

    test("activating a campaign publishes an ACTIVATED event") {
        val created = CreateCampaign.generate.request {
            body {
                name = Arb.constant("Flash deal")
                discountPercentage = Arb.constant(30L)
                productIds = Arb.constant(emptyList())
            }
        }.call()
        created.shouldBeInstanceOf<CreateCampaign.Response201>()
        // Drain the CREATED event so the assertion below is about the activation.
        CampaignEvents.generate.listen { expecting { it.eventType shouldBe CampaignEventType.CREATED } }

        val activated = ActivateCampaign.generate.request {
            path { id = Arb.constant(created.body.id) }
        }.call()
        activated.shouldBeInstanceOf<ActivateCampaign.Response200>()
        activated.body.status shouldBe CampaignStatus.ACTIVE

        CampaignEvents.generate.listen {
            expecting { event ->
                event.campaignId shouldBe created.body.id
                event.eventType shouldBe CampaignEventType.ACTIVATED
            }
        }
    }

    test("a CampaignEvent sent through the channel round-trips over the topic") {
        // send() takes the destination topic (and an optional key); omit it to fall back to the
        // channel context's defaultTopic.
        val sent = CampaignEvents.generate.message {
            eventType = Arb.constant(CampaignEventType.ENDED)
        }.send(CAMPAIGN_EVENTS_TOPIC)

        CampaignEvents.generate.listen {
            expecting { received -> received shouldBe sent }
        }
    }
})
