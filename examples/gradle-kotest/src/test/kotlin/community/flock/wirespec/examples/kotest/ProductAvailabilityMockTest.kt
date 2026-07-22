package community.flock.wirespec.examples.kotest

import community.flock.wirespec.examples.kotest.generated.endpoint.CreateProduct
import community.flock.wirespec.examples.kotest.generated.endpoint.GetProductAvailability
import community.flock.wirespec.examples.kotest.generated.endpoint.GetStock
import community.flock.wirespec.examples.kotest.generated.kotest.call
import community.flock.wirespec.examples.kotest.generated.kotest.generate
import community.flock.wirespec.examples.kotest.generated.kotest.mock
import community.flock.wirespec.examples.kotest.generated.model.Availability
import community.flock.wirespec.examples.kotest.generated.model.ProductId
import community.flock.wirespec.examples.kotest.generated.model.StockLevel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Mocks a downstream dependency end-to-end with the Kotest response-side DSL. The app's
 * `GET /products/{id}/availability` calls the inventory service (the generated `GetStock` client);
 * this spec stubs that call with `GetStock.generate.response200 { … }.mock { req -> req.path.sku == … }`
 * and asserts the app's response reflects the canned stock.
 *
 * `WirespecMockExtension` is registered once in [ProjectConfig] against the shared [inventoryMockServer];
 * here the spec only points the app's `inventory.base-url` at it (via `@DynamicPropertySource`, before
 * the context boots). `.call()` drives the app over real HTTP while `.mock { }` stubs the downstream.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductAvailabilityMockTest :
    FunSpec({

        test("availability reflects the mocked downstream stock") {
            val sku = "SKU-001"
            val productId = createProduct(sku)

            // Only requests for SKU-001 get this canned 200 — the predicate reads the typed request.
            GetStock.generate
                .response200 {
                    body = StockLevel.generate {
                        sku(sku)
                        available(7L)
                        warehouse("EU-WEST")
                    }
                }
                .mock { req -> req.path.sku == sku }

            val availability = GetProductAvailability.generate
                .request {
                    path { id(productId) }
                }
                .call()

            availability.shouldBeInstanceOf<GetProductAvailability.Response200>()
            availability.body.inStock shouldBe true
            availability.body.available shouldBe 7L
        }

        test("the mock predicate routes each SKU to its own stock") {
            val outOfStock = createProduct("SKU-OOS")
            val inStock = createProduct("SKU-IN")

            GetStock.generate
                .response200 {
                    body = StockLevel.generate {
                        sku("SKU-OOS")
                        available(0L)
                        warehouse("EU-WEST")
                    }
                }
                .mock { req -> req.path.sku == "SKU-OOS" }
            GetStock.generate
                .response200 {
                    body = StockLevel.generate {
                        sku("SKU-IN")
                        available(42L)
                        warehouse("EU-WEST")
                    }
                }
                .mock { req -> req.path.sku == "SKU-IN" }

            availabilityOf(outOfStock).let {
                it.available shouldBe 0L
                it.inStock shouldBe false
            }
            availabilityOf(inStock).let {
                it.available shouldBe 42L
                it.inStock shouldBe true
            }
        }
    }) {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun inventoryProperties(registry: DynamicPropertyRegistry) {
            registry.add("inventory.base-url") { inventoryMockServer.baseUrl }
        }
    }
}

private suspend fun createProduct(sku: String): ProductId {
    val created = CreateProduct.generate.request {
        body {
            sku(sku)
            name("Product $sku")
            price(29.95)
        }
    }.call()
    created.shouldBeInstanceOf<CreateProduct.Response201>()
    return created.body.id
}

private suspend fun availabilityOf(productId: ProductId): Availability {
    val response = GetProductAvailability.generate.request {
        path { id(productId) }
    }.call()
    response.shouldBeInstanceOf<GetProductAvailability.Response200>()
    return response.body
}
