package community.flock.wirespec.examples.kotest.downstream

import community.flock.wirespec.examples.kotest.generated.endpoint.GetStock
import community.flock.wirespec.integration.java.transport.HttpTransportation
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Calls the downstream inventory service over HTTP, driving the Wirespec-generated [GetStock]
 * *client* (`GetStock.Handler` is both a server and a client edge; this uses the client side). The
 * base URL comes from the `inventory.base-url` property, so tests can point it at a mock server.
 *
 * Requests/responses are (de)serialized with the shared [Wirespec.Serialization] bean, so the bytes
 * match what the mock's `GetStock.generate.response200 { … }.mock { … }` stub produces.
 */
@Component
class InventoryClient(
    serialization: Wirespec.Serialization,
    @Value("\${inventory.base-url}") baseUrl: String,
) {
    private val edge = GetStock.Handler.client(serialization)
    private val transportation = HttpTransportation(baseUrl)

    suspend fun getStock(sku: String): GetStock.Response<*> =
        edge.from(transportation.transport(edge.to(GetStock.Request(sku))))
}
