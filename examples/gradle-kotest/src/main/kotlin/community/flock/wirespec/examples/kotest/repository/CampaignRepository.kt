package community.flock.wirespec.examples.kotest.repository

import community.flock.wirespec.examples.kotest.generated.model.Campaign
import community.flock.wirespec.examples.kotest.generated.model.CampaignId
import community.flock.wirespec.examples.kotest.generated.model.CampaignStatus
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/** In-memory store for [Campaign]s, keyed by their refined [CampaignId]. */
@Repository
class CampaignRepository {
    private val store = ConcurrentHashMap<String, Campaign>()

    suspend fun save(campaign: Campaign): Campaign = campaign.also { store[it.id.value] = it }

    suspend fun findById(id: CampaignId): Campaign? = store[id.value]

    suspend fun findAll(status: CampaignStatus?): List<Campaign> = store.values
        .filter { status == null || it.status == status }
        .toList()

    suspend fun delete(id: CampaignId): Boolean = store.remove(id.value) != null
}
