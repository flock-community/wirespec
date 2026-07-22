package community.flock.wirespec.examples.kotest.kafka

import community.flock.wirespec.examples.kotest.generated.channel.CampaignEvents
import community.flock.wirespec.examples.kotest.generated.model.CampaignEvent
import community.flock.wirespec.kotlin.Wirespec
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.typeOf

/**
 * Consumes [CampaignEvent]s from Kafka and fans them out to handlers registered through the
 * Wirespec-generated [CampaignEvents.Listener]. Decodes with the shared [Wirespec.Serialization] bean.
 *
 * This is the app's own consumer (group `campaign-app`); the scenario tests read the same topic under
 * a different group, so both observe every message independently.
 */
@Component
class CampaignEventConsumer(
    private val serialization: Wirespec.Serialization,
) : CampaignEvents.Listener {

    private val log = LoggerFactory.getLogger(javaClass)
    private val handlers = CopyOnWriteArrayList<(CampaignEvent) -> Unit>()

    override fun campaignEvents(handler: (CampaignEvent) -> Unit) {
        handlers.add(handler)
    }

    @KafkaListener(topics = [CAMPAIGN_EVENTS_TOPIC], groupId = "campaign-app")
    fun onMessage(body: ByteArray) {
        val event = serialization.deserializeBody<CampaignEvent>(body, typeOf<CampaignEvent>())
        log.info("Consumed CampaignEvent: {}", event)
        handlers.forEach { it(event) }
    }
}
