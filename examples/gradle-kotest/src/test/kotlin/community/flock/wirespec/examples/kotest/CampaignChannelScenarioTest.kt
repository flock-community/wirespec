package community.flock.wirespec.examples.kotest

import community.flock.wirespec.examples.kotest.generated.channel.CampaignEvents
import community.flock.wirespec.examples.kotest.generated.endpoint.ActivateCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.CreateCampaign
import community.flock.wirespec.examples.kotest.generated.kotest.call
import community.flock.wirespec.examples.kotest.generated.kotest.generate
import community.flock.wirespec.examples.kotest.generated.kotest.send
import community.flock.wirespec.examples.kotest.generated.model.CampaignEvent
import community.flock.wirespec.examples.kotest.generated.model.CampaignEventType
import community.flock.wirespec.examples.kotest.generated.model.CampaignInput
import community.flock.wirespec.examples.kotest.generated.model.CampaignStatus
import community.flock.wirespec.examples.kotest.kafka.CAMPAIGN_EVENTS_TOPIC
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.testContextManager
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.RandomSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import java.time.Duration.ofMillis
import java.util.UUID
import kotlin.reflect.typeOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * End-to-end messaging scenarios over an in-JVM Kafka broker (`@EmbeddedKafka`, no Docker). The
 * Wirespec DSL drives the **send** direction only (`generate.message { … }.send()`); asserting on what
 * the app published is done with a plain Kafka consumer ([awaitEvent]). The endpoint and channel
 * ambients compose, so a scenario can act on the REST side that emits events then assert on the topic.
 *
 * `@EmbeddedKafka` starts the broker before the context loads and points the app's
 * `spring.kafka.bootstrap-servers` at it, which the channel extension resolves per spec.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
    partitions = 1,
    topics = [CAMPAIGN_EVENTS_TOPIC],
    bootstrapServersProperty = "spring.kafka.bootstrap-servers",
)
class CampaignChannelScenarioTest :
    FunSpec({

        test("Hello") {

            val x = CampaignInput.generate { }
            val l = x.generate(RandomSource.default()).take(10).map { it.value }.toList()

            println(l)
        }

        test("creating a campaign publishes a CREATED event") {
            val created = CreateCampaign.generate
                .request {
                    body {
                        name("Autumn promo")
                        discountPercentage(20L)
                    }
                }.call()
                .shouldBeInstanceOf<CreateCampaign.Response201>()
                .also {
                    it.body.status shouldBe CampaignStatus.DRAFT
                }

            awaitEvent<CampaignEvent>(CAMPAIGN_EVENTS_TOPIC) { it.campaignId == created.body.id && it.eventType == CampaignEventType.CREATED }
                .should { it.discountPercentage shouldBe 20L }
        }

        test("activating a campaign publishes an ACTIVATED event") {
            val created = CreateCampaign.generate.request {
                body {
                    name("Flash deal")
                    discountPercentage(30L)
                    productIds(emptyList())
                }
            }.call()
            created.shouldBeInstanceOf<CreateCampaign.Response201>()

            val activated = ActivateCampaign.generate.request {
                path { id(created.body.id) }
            }.call()
            activated.shouldBeInstanceOf<ActivateCampaign.Response200>()
            activated.body.status shouldBe CampaignStatus.ACTIVE

            // Filtering by campaignId + eventType isolates this campaign's ACTIVATED event from the
            // CREATED one (and from other tests sharing the broker), so no draining is needed.
            val event =
                awaitEvent<CampaignEvent>(CAMPAIGN_EVENTS_TOPIC) { it.campaignId == created.body.id && it.eventType == CampaignEventType.ACTIVATED }
            event.campaignId shouldBe created.body.id
        }

        test("a CampaignEvent sent through the channel round-trips over the topic") {
            // send() takes the destination topic (and an optional key); omit it to fall back to the
            // channel object's simple name.
            val sent = CampaignEvents.generate
                .message {
                    eventType(CampaignEventType.ENDED)
                }
                .send(CAMPAIGN_EVENTS_TOPIC)

            val received =
                awaitEvent<CampaignEvent>(CAMPAIGN_EVENTS_TOPIC) { it.campaignId == sent.campaignId && it.eventType == CampaignEventType.ENDED }
            received shouldBe sent
        }
    })

/**
 * Poll [topic] with a throwaway consumer group (from `earliest`) until an event of type [T] satisfying
 * [predicate] arrives, or [timeout] elapses. Bodies are decoded with the app's own
 * `Wirespec.Serialization` bean (the mirror of what the publisher wrote) so equality with a sent payload holds.
 */
private suspend inline fun <reified T : Any> awaitEvent(
    topic: String,
    timeout: Duration = 10.seconds,
    crossinline predicate: (T) -> Boolean,
): T {
    val applicationContext = testContextManager().testContext.applicationContext
    val serialization = applicationContext.getBean(Wirespec.Serialization::class.java)
    val bootstrapServers = applicationContext.environment.getProperty("spring.kafka.bootstrap-servers")
        ?: error("spring.kafka.bootstrap-servers is not set in the test context")

    return withContext(Dispatchers.IO) {
        eventConsumer(bootstrapServers).use { consumer ->
            consumer.subscribe(listOf(topic))
            val deadline = System.nanoTime() + timeout.inWholeNanoseconds
            while (System.nanoTime() < deadline) {
                consumer.poll(ofMillis(500)).forEach { record ->
                    val event = serialization.deserializeBody<T>(record.value(), typeOf<T>())
                    if (predicate(event)) return@use event
                }
            }
            error("No matching ${T::class.simpleName} arrived on $topic within $timeout")
        }
    }
}

private fun eventConsumer(bootstrapServers: String): KafkaConsumer<String, ByteArray> = KafkaConsumer(
    mapOf(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        ConsumerConfig.GROUP_ID_CONFIG to "kotest-assert-${UUID.randomUUID()}",
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java.name,
    ),
)
