package community.flock.wirespec.examples.kotest

import community.flock.wirespec.examples.kotest.generated.endpoint.CreateCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.CreateProduct
import community.flock.wirespec.examples.kotest.generated.endpoint.GetCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.GetProduct
import community.flock.wirespec.examples.kotest.generated.kotest.call
import community.flock.wirespec.examples.kotest.generated.kotest.generate
import community.flock.wirespec.integration.kotest.dsl.draw
import community.flock.wirespec.examples.kotest.generated.model.CampaignId
import community.flock.wirespec.examples.kotest.generated.model.Product
import community.flock.wirespec.examples.kotest.generated.model.ProductId
import community.flock.wirespec.integration.kotest.WirespecEndpointExtension
import community.flock.wirespec.integration.kotest.transport.HttpTransportation
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.testContextManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID

/**
 * End-to-end HTTP scenarios driven entirely through the Wirespec-generated Kotest DSL.
 *
 * The Spring context (a real embedded server on a random port) is wired in by the Kotest
 * [SpringExtension]. The framework-agnostic [WirespecEndpointExtension] is registered in the spec
 * body; its `suspend` factories read the port and the app's `Wirespec.Serialization` bean from that
 * context (via `testContextManager()`), so `SomeEndpoint.generate.request { … }.call()` transports
 * over the server and validates every reply against the contract.
 *
 * Kafka isn't needed here, so its listener is disabled; event publishing is fire-and-forget.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.kafka.listener.auto-startup=false"],
)
@ApplyExtension(SpringExtension::class)
class CampaignEndpointScenarioTest : FunSpec({

    // The endpoint extension is framework-agnostic; the Spring wiring lives here, in its suspend
    // factories, which read the loaded test context via `testContextManager()`.
    extension(
        WirespecEndpointExtension(
            serialization = {
                testContextManager().testContext.applicationContext.getBean(Wirespec.Serialization::class.java)
            },
            transportation = {
                val context = testContextManager().testContext.applicationContext
                val port = context.environment.getProperty("local.server.port") ?: error("Embedded server port not available")
                HttpTransportation("http://localhost:$port")
            },
        ),
    )

    test("CreateProduct returns a 201 echoing the generated body") {
        val response = CreateProduct.generate.request {
            body {
                sku = Arb.constant("SKU-001")
                name = Arb.constant("Wireless mouse")
                price = Arb.constant(29.95)
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
            body { sku = Arb.constant("PINNED-SKU") }
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
        val response = CreateProduct.generate.response201 { body = Arb.constant(product) }.draw()

        response.shouldBeInstanceOf<CreateProduct.Response201>()
        response.body shouldBe product
    }

    test("GetProduct on an unknown id returns a 404 with a contract Error") {
        val response = GetProduct.generate.request {
            path { id = Arb.constant(ProductId(UUID.randomUUID().toString())) }
        }.call()

        response.shouldBeInstanceOf<GetProduct.Response404>()
        response.body.code shouldBe 404
    }

    test("a created campaign can be fetched back by its id") {
        val created = CreateCampaign.generate.request {
            body {
                name = Arb.constant("Summer sale")
                discountPercentage = Arb.constant(15L)
                productIds = Arb.constant(emptyList())
            }
        }.call()
        created.shouldBeInstanceOf<CreateCampaign.Response201>()
        val id: CampaignId = created.body.id

        val fetched = GetCampaign.generate.request {
            path { this.id = Arb.constant(id) }
        }.call()

        fetched.shouldBeInstanceOf<GetCampaign.Response200>()
        fetched.body.id shouldBe id
        fetched.body.name shouldBe "Summer sale"
    }
})
