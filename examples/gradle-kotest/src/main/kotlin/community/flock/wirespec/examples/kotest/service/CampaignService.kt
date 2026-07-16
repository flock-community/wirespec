package community.flock.wirespec.examples.kotest.service

import community.flock.wirespec.examples.kotest.generated.model.Campaign
import community.flock.wirespec.examples.kotest.generated.model.CampaignEvent
import community.flock.wirespec.examples.kotest.generated.model.CampaignEventType
import community.flock.wirespec.examples.kotest.generated.model.CampaignId
import community.flock.wirespec.examples.kotest.generated.model.CampaignInput
import community.flock.wirespec.examples.kotest.generated.model.CampaignStatus
import community.flock.wirespec.examples.kotest.kafka.CampaignEventPublisher
import community.flock.wirespec.examples.kotest.repository.CampaignRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CampaignService(
    private val repository: CampaignRepository,
    private val publisher: CampaignEventPublisher,
) {
    suspend fun create(input: CampaignInput): Campaign {
        val campaign = Campaign(
            id = CampaignId(UUID.randomUUID().toString()),
            name = input.name,
            status = CampaignStatus.DRAFT,
            discountPercentage = input.discountPercentage,
            productIds = input.productIds,
        )
        repository.save(campaign)
        publisher.publish(campaign, CampaignEventType.CREATED)
        return campaign
    }

    suspend fun get(id: CampaignId): Campaign? = repository.findById(id)

    suspend fun list(status: CampaignStatus?): List<Campaign> = repository.findAll(status)

    suspend fun update(id: CampaignId, input: CampaignInput): Campaign? {
        val existing = repository.findById(id) ?: return null
        return repository.save(
            existing.copy(
                name = input.name,
                discountPercentage = input.discountPercentage,
                productIds = input.productIds,
            ),
        )
    }

    suspend fun activate(id: CampaignId): Campaign? {
        val existing = repository.findById(id) ?: return null
        val activated = repository.save(existing.copy(status = CampaignStatus.ACTIVE))
        publisher.publish(activated, CampaignEventType.ACTIVATED)
        return activated
    }

    suspend fun delete(id: CampaignId): Boolean = repository.delete(id)

    private fun CampaignEventPublisher.publish(campaign: Campaign, type: CampaignEventType) =
        campaignEvents(
            CampaignEvent(
                campaignId = campaign.id,
                eventType = type,
                discountPercentage = campaign.discountPercentage,
            ),
        )
}
