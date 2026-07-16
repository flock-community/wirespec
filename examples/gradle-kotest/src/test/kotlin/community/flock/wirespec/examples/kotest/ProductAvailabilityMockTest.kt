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
import community.flock.wirespec.examples.kotest.mock.WireMockMockServer
import community.flock.wirespec.integration.kotest.WirespecMockExtension
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.testContextManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Mocks a downstream dependency end-to-end with the Kotest response-side DSL. The app's
 * `GET /products/{id}/availability` calls out to the inventory service (`inventory.ws` → the
 * generated `GetStock` client); this spec stubs that call with
 * `GetStock.generate.response200 { … }.mock { req -> req.path.sku == … }` and asserts the app's
 * response reflects the canned stock.
 *
 * The mock server is a real HTTP server ([WireMockMockServer]): it is started in the companion so
 * its `baseUrl` can be fed to the app's `inventory.base-url` via `@DynamicPropertySource` before the
 * Spring context boots. [WirespecMockExtension] is registered spec-locally (it needs that eager
 * server) and composes with the global endpoint extension from [ProjectConfig], so `.call()` drives
 * the app over real HTTP while `.mock { }` stubs the downstream — the managed form resets stubs
 * between tests and stops the server after the spec.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductAvailabilityMockTest : FunSpec() {

    init {
        extension(
            WirespecMockExtension(
                serialization = { mockSerialization() },
                server = { mockServer },
            ),
        )

        test("availability reflects the mocked downstream stock") {
            val sku = "SKU-001"
            val productId = createProduct(sku)

            // Only requests for SKU-001 get this canned 200 — the predicate reads the typed request.
            GetStock.generate.response200 {
                body = StockLevel.generate {
                    this.sku = Arb.constant(sku)
                    available = Arb.constant(7L)
                    warehouse = Arb.constant("EU-WEST")
                }
            }.mock { req -> req.path.sku == sku }

            val availability = GetProductAvailability.generate.request {
                path { id = Arb.constant(productId) }
            }.call()

            availability.shouldBeInstanceOf<GetProductAvailability.Response200>()
            availability.body.inStock shouldBe true
            availability.body.available shouldBe 7L
        }

        test("the mock predicate routes each SKU to its own stock") {
            val outOfStock = createProduct("SKU-OOS")
            val inStock = createProduct("SKU-IN")

            GetStock.generate
                .response200 {
                    body = StockLevel.generate { sku = Arb.constant("SKU-OOS"); available = Arb.constant(0L); warehouse = Arb.constant("EU-WEST") }
                }
                .mock { req -> req.path.sku == "SKU-OOS" }
            GetStock.generate
                .response200 {
                    body = StockLevel.generate { sku = Arb.constant("SKU-IN"); available = Arb.constant(42L); warehouse = Arb.constant("EU-WEST") }
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
    }

    companion object {
        private val mockServer = WireMockMockServer.start()

        @JvmStatic
        @DynamicPropertySource
        fun inventoryProperties(registry: DynamicPropertyRegistry) {
            registry.add("inventory.base-url") { mockServer.baseUrl }
        }
    }
}

private suspend fun createProduct(sku: String): ProductId {
    val created = CreateProduct.generate.request {
        body {
            this.sku = Arb.constant(sku)
            name = Arb.constant("Product $sku")
            price = Arb.constant(29.95)
        }
    }.call()
    created.shouldBeInstanceOf<CreateProduct.Response201>()
    return created.body.id
}

private suspend fun availabilityOf(productId: ProductId): Availability {
    val response = GetProductAvailability.generate.request {
        path { id = Arb.constant(productId) }
    }.call()
    response.shouldBeInstanceOf<GetProductAvailability.Response200>()
    return response.body
}

private suspend fun mockSerialization(): Wirespec.Serialization =
    testContextManager().testContext.applicationContext.getBean(Wirespec.Serialization::class.java)
