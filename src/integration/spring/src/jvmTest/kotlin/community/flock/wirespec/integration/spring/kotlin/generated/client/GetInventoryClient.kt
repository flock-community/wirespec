package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetInventory



interface GetInventoryClient {
  suspend fun getInventory(): GetInventory.Response<*>
}