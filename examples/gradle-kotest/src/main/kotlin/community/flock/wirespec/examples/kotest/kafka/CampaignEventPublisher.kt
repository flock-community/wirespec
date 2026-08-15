package community.flock.wirespec.examples.kotest.kafka

import community.flock.wirespec.examples.kotest.generated.channel.CampaignEvents
import community.flock.wirespec.examples.kotest.generated.model.CampaignEvent
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import kotlin.reflect.typeOf

/**
 * Implements the generated [CampaignEvents.Sender] over Kafka: the typed
 * [CampaignEvent] is serialized to JSON with the shared [Wirespec.Serialization]
 * and produced to [CAMPAIGN_EVENTS_TOPIC], keyed by campaign id.
 */
@OptIn(ExperimentalStdlibApi::class)
@Component
class CampaignEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val serialization: Wirespec.Serialization,
) : CampaignEvents.Sender {

    override fun campaignEvents(message: CampaignEvent) {
        val json = String(serialization.serializeBody(message, typeOf<CampaignEvent>()))
        kafkaTemplate.send(CAMPAIGN_EVENTS_TOPIC, message.campaignId.value, json)
    }
}
