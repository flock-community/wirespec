package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DeleteOrder



interface DeleteOrderClient {
  suspend fun deleteOrder(orderId: Long): DeleteOrder.Response<*>
}