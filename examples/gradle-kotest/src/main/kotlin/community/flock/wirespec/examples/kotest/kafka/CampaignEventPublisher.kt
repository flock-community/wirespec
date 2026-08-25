package community.flock.wirespec.examples.kotest.kafka

import community.flock.wirespec.examples.kotest.generated.channel.CampaignEvents
import community.flock.wirespec.examples.kotest.generated.model.CampaignEvent
import community.flock.wirespec.kotlin.Wirespec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import kotlin.reflect.typeOf

@Component
class CampaignEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
    private val serialization: Wirespec.Serialization,
) : CampaignEvents.Sender {

    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun campaignEvents(message: CampaignEvent) {
        val body = serialization.serializeBody(message, typeOf<CampaignEvent>())
        scope.launch {
            runCatching { kafkaTemplate.send(CAMPAIGN_EVENTS_TOPIC, message.campaignId.value, body) }
                .onFailure { log.warn("Failed to publish CampaignEvent to Kafka: ${it.message}") }
        }
    }
}
