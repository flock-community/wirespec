package community.flock.wirespec.examples.kotest

import community.flock.wirespec.examples.kotest.generated.channel.CampaignEvents
import community.flock.wirespec.examples.kotest.generated.endpoint.ActivateCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.CreateCampaign
import community.flock.wirespec.examples.kotest.generated.kotest.call
import community.flock.wirespec.examples.kotest.generated.kotest.generate
import community.flock.wirespec.examples.kotest.generated.model.CampaignEventType
import community.flock.wirespec.examples.kotest.kafka.CampaignEventConsumer
import community.flock.wirespec.examples.kotest.support.CampaignTestEnvironment
import community.flock.wirespec.integration.kotest.WirespecExtension
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import kotlin.time.Duration.Companion.seconds

/**
 * Kafka channel scenarios driven by the generated `CampaignEvents.generate.call { … }` DSL.
 *
 * The DSL publishes/consumes through the embedded broker, demonstrating both
 * directions: the app reacting to an endpoint call by emitting an event the DSL then
 * `expecting()`s, and the DSL `send`ing an event the app's `@KafkaListener` consumes.
 *
 * `beforeEach` repositions the shared channel consumer at the log end so each scenario
 * only observes the events it produces.
 */
class CampaignChannelScenarioTest : FunSpec({

    extension(
        WirespecExtension(
            endpoint = CampaignTestEnvironment.endpointContext,
            channel = CampaignTestEnvironment.channelContext,
        ),
    )

    beforeEach { CampaignTestEnvironment.watchChannelFromNow() }

    test("creating then activating a campaign emits the matching CampaignEvents") {
        val createResponse = CreateCampaign.generate.request {
            body {
                name = Arb.constant("Black Friday")
                discountPercentage = Arb.constant(40L)
                productIds = Arb.constant(emptyList<String>())
            }
        }.call()
        createResponse.shouldBeInstanceOf<CreateCampaign.Response201>()
        val campaign = createResponse.body

        CampaignEvents.generate.call {
            expecting { event ->
                event.campaignId shouldBe campaign.id
                event.eventType shouldBe CampaignEventType.CREATED
                event.discountPercentage shouldBe 40L
            }
        }

        ActivateCampaign.generate.request {
            path { id = Arb.constant(campaign.id) }
        }.call().shouldBeInstanceOf<ActivateCampaign.Response200>()

        CampaignEvents.generate.call {
            expecting { event ->
                event.campaignId shouldBe campaign.id
                event.eventType shouldBe CampaignEventType.ACTIVATED
            }
        }
    }

    test("an event sent through the DSL is consumed by the application listener") {
        val sent = CampaignEvents.generate.call {
            send {
                eventType = Arb.constant(CampaignEventType.ENDED)
                discountPercentage = Arb.constant(0L)
            }
        }

        val listener = CampaignTestEnvironment.application.getBean(CampaignEventConsumer::class.java)
        eventually(10.seconds) {
            listener.receivedEvents().any {
                it.campaignId == sent.campaignId && it.eventType == CampaignEventType.ENDED
            } shouldBe true
        }
    }
})
