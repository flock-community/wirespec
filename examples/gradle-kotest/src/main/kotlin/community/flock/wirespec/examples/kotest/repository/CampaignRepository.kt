package community.flock.wirespec.examples.kotest.repository

import community.flock.wirespec.examples.kotest.generated.model.Campaign
import community.flock.wirespec.examples.kotest.generated.model.CampaignId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Repository

@Repository
class CampaignRepository {

    private val mutex = Mutex()
    private val store = mutableMapOf<CampaignId, Campaign>()

    suspend fun findAll(): List<Campaign> = mutex.withLock { store.values.toList() }

    suspend fun findById(id: CampaignId): Campaign? = mutex.withLock { store[id] }

    suspend fun save(campaign: Campaign): Campaign = mutex.withLock {
        store[campaign.id] = campaign
        campaign
    }

    suspend fun delete(id: CampaignId): Boolean = mutex.withLock { store.remove(id) != null }
}
