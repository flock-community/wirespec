package community.flock.wirespec.examples.kotest.downstream

import community.flock.wirespec.examples.kotest.generated.endpoint.GetStock
import community.flock.wirespec.integration.java.transport.HttpTransportation
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Calls the downstream inventory service over HTTP via the Wirespec-generated [GetStock] client edge.
 * The base URL comes from `inventory.base-url`, so tests can point it at a mock server. Uses the
 * shared [Wirespec.Serialization] bean so the bytes match what the mock stub produces.
 */
@Component
class InventoryClient(
    serialization: Wirespec.Serialization,
    @Value("\${inventory.base-url}") baseUrl: String,
) {
    private val edge = GetStock.Handler.client(serialization)
    private val transportation = HttpTransportation(baseUrl)

    suspend fun getStock(sku: String): GetStock.Response<*> = edge.from(transportation.transport(edge.to(GetStock.Request(sku))))
}
