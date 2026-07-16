package community.flock.wirespec.examples.kotest.controller

import community.flock.wirespec.examples.kotest.downstream.InventoryClient
import community.flock.wirespec.examples.kotest.generated.endpoint.GetProductAvailability
import community.flock.wirespec.examples.kotest.generated.endpoint.GetStock
import community.flock.wirespec.examples.kotest.generated.model.Availability
import community.flock.wirespec.examples.kotest.generated.model.Error
import community.flock.wirespec.examples.kotest.service.ProductService
import org.springframework.web.bind.annotation.RestController

/**
 * Serves `GET /products/{id}/availability` by looking up the product, then calling the downstream
 * inventory service (via [InventoryClient]) for its SKU and mapping the stock into an [Availability].
 * The downstream call is what the Kotest `.mock { req -> … }` DSL stubs in `ProductAvailabilityMockTest`.
 */
@RestController
class AvailabilityController(
    private val products: ProductService,
    private val inventory: InventoryClient,
) : GetProductAvailability.Handler {

    override suspend fun getProductAvailability(request: GetProductAvailability.Request): GetProductAvailability.Response<*> {
        val product = products.get(request.path.id)
            ?: return GetProductAvailability.Response404(Error(code = 404, message = "Product not found: ${request.path.id.value}"))

        return when (val stock = inventory.getStock(product.sku)) {
            is GetStock.Response200 -> GetProductAvailability.Response200(
                Availability(
                    productId = product.id,
                    inStock = stock.body.available > 0,
                    available = stock.body.available,
                ),
            )
            else -> GetProductAvailability.Response404(Error(code = 404, message = "No stock for sku: ${product.sku}"))
        }
    }
}
