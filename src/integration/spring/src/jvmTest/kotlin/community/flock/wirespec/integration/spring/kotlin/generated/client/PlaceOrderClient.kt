package community.flock.wirespec.integration.spring.kotlin.generated.client

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.PlaceOrder

import community.flock.wirespec.integration.spring.kotlin.generated.model.Order

interface PlaceOrderClient {
  suspend fun placeOrder(body: Order): PlaceOrder.Response<*>
}