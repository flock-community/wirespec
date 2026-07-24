package community.flock.wirespec.examples.kotest

import community.flock.wirespec.examples.kotest.generated.endpoint.CreateCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.CreateProduct
import community.flock.wirespec.examples.kotest.generated.endpoint.GetCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.GetProduct
import community.flock.wirespec.examples.kotest.generated.kotest.call
import community.flock.wirespec.examples.kotest.generated.kotest.generate
import community.flock.wirespec.examples.kotest.generated.model.CampaignId
import community.flock.wirespec.examples.kotest.generated.model.Product
import community.flock.wirespec.examples.kotest.generated.model.ProductId
import community.flock.wirespec.integration.kotest.dsl.draw
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID

/**
 * End-to-end HTTP scenarios driven through the Wirespec-generated Kotest DSL. Extensions are
 * registered once in [ProjectConfig], so this spec only declares `@SpringBootTest` (a real embedded
 * server on a random port, which the endpoint extension reads for port and serialization).
 * Kafka's listener is disabled here since it isn't needed; event publishing is fire-and-forget.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.kafka.listener.auto-startup=false"],
)
class CampaignEndpointScenarioTest :
    FunSpec({

        test("CreateProduct returns a 201 echoing the generated body") {
            val response = CreateProduct.generate.request {
                body {
                    sku("SKU-001")
                    name("Wireless mouse")
                    price(29.95)
                }
            }.call()

            response.shouldBeInstanceOf<CreateProduct.Response201>()
            response.body.sku shouldBe "SKU-001"
            response.body.name shouldBe "Wireless mouse"
            response.body.price shouldBe 29.95
        }

        test("request builds a random CreateProduct.Request, pinning only what you set") {
            // `request { }` returns a `Gen<Request>`; draw one to inspect it.
            val request = CreateProduct.generate.request {
                body { sku("PINNED-SKU") }
            }.draw()

            // The pinned field is exactly what you set; the rest is generated but present.
            request.body.sku shouldBe "PINNED-SKU"
            request.body.name.shouldBeInstanceOf<String>()
        }

        test("response201 builds a random CreateProduct.Response201, pinning only what you set") {
            val product = Product(
                id = ProductId(UUID.randomUUID().toString()),
                sku = "SKU-042",
                name = "Mechanical keyboard",
                price = 119.0,
            )
            val response = CreateProduct.generate.response201 { body(product) }.draw()

            response.shouldBeInstanceOf<CreateProduct.Response201>()
            response.body shouldBe product
        }

        test("GetProduct on an unknown id returns a 404 with a contract Error") {
            val response = GetProduct.generate.request {
                path { id(ProductId(UUID.randomUUID().toString())) }
            }.call()

            response.shouldBeInstanceOf<GetProduct.Response404>()
            response.body.code shouldBe 404
        }

        test("a created campaign can be fetched back by its id") {
            val created = CreateCampaign.generate.request {
                body {
                    name("Summer sale")
                    discountPercentage(15L)
                    productIds(emptyList())
                }
            }.call()
            created.shouldBeInstanceOf<CreateCampaign.Response201>()
            val id: CampaignId = created.body.id

            val fetched = GetCampaign.generate.request {
                path { id(id) }
            }.call()

            fetched.shouldBeInstanceOf<GetCampaign.Response200>()
            fetched.body.id shouldBe id
            fetched.body.name shouldBe "Summer sale"
        }
    })
