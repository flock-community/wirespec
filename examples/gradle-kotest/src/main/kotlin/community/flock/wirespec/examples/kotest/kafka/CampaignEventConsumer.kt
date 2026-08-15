package community.flock.wirespec.examples.kotest.kafka

import community.flock.wirespec.examples.kotest.generated.channel.CampaignEvents
import community.flock.wirespec.examples.kotest.generated.model.CampaignEvent
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.typeOf

/**
 * Implements the generated [CampaignEvents.Listener] over Kafka. Callers register
 * handlers through `campaignEvents { … }`; the `@KafkaListener` deserializes each
 * record with the shared [Wirespec.Serialization] and dispatches it to them. Every
 * consumed event is also retained for inspection.
 */
@OptIn(ExperimentalStdlibApi::class)
@Component
class CampaignEventConsumer(
    private val serialization: Wirespec.Serialization,
) : CampaignEvents.Listener {

    private val handlers = CopyOnWriteArrayList<(CampaignEvent) -> Unit>()
    private val received = CopyOnWriteArrayList<CampaignEvent>()

    override fun campaignEvents(handler: (CampaignEvent) -> Unit) {
        handlers.add(handler)
    }

    @KafkaListener(topics = [CAMPAIGN_EVENTS_TOPIC], groupId = "campaign-events-app")
    fun onMessage(payload: String) {
        val event = serialization.deserializeBody<CampaignEvent>(payload.toByteArray(), typeOf<CampaignEvent>())
        received.add(event)
        handlers.forEach { it(event) }
    }

    /** Every [CampaignEvent] consumed so far, in arrival order. */
    fun receivedEvents(): List<CampaignEvent> = received.toList()
}
