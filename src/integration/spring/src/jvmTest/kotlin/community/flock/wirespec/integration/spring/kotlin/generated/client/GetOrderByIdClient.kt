package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetOrderById



interface GetOrderByIdClient {
  suspend fun getOrderById(orderId: Long): GetOrderById.Response<*>
}