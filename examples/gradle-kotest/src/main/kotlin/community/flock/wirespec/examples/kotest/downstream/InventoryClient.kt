package community.flock.wirespec.examples.kotest.downstream

import community.flock.wirespec.examples.kotest.generated.endpoint.GetStock
import community.flock.wirespec.integration.jvm.transport.HttpTransportation
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class InventoryClient(
    serialization: Wirespec.Serialization,
    @Value("\${inventory.base-url}") baseUrl: String,
) {
    private val edge = GetStock.Handler.client(serialization)
    private val transportation = HttpTransportation(baseUrl)

    suspend fun getStock(sku: String): GetStock.Response<*> = edge.from(transportation.transport(edge.to(GetStock.Request(sku))))
}
