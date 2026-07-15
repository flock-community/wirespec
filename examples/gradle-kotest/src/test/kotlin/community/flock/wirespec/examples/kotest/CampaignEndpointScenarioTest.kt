package community.flock.wirespec.examples.kotest

import community.flock.wirespec.examples.kotest.generated.endpoint.CreateCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.CreateProduct
import community.flock.wirespec.examples.kotest.generated.endpoint.GetCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.GetProduct
import community.flock.wirespec.examples.kotest.generated.kotest.call
import community.flock.wirespec.examples.kotest.generated.kotest.generate
import community.flock.wirespec.examples.kotest.generated.model.Product
import community.flock.wirespec.examples.kotest.generated.model.ProductId
import community.flock.wirespec.examples.kotest.support.CampaignTestEnvironment
import community.flock.wirespec.integration.kotest.WirespecExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant

/**
 * Endpoint scenarios driven by the generated `<Endpoint>.generate.call { … }` DSL. Each call
 * builds its request body/path with kotest `Arb`s (unset fields are generated from
 * the contract), sends it over real HTTP to the running app, and validates the typed
 * response variant against the Wirespec contract.
 *
 * The endpoint transport comes from the shared `CampaignTestEnvironment`, handed to
 * the ambient via the `WirespecExtension` this spec registers.
 */
class CampaignEndpointScenarioTest : FunSpec({
    extension(WirespecExtension(endpoint = CampaignTestEnvironment.endpointContext))

    test("CreateProduct returns a 201 echoing the generated body") {
        CreateProduct.generate.call {
            body { name = Arb.constant("Wireless Mouse") }
            expecting<CreateProduct.Response201> { response ->
                response.body.name shouldBe "Wireless Mouse"
                response.body.id.value.shouldBeUuid()
            }
        }
    }

    test("request builds a random CreateProduct.Request, pinning only what you set") {
        // Same scope block as `call { … }`, but it materialises the request instead of sending it.
        // Unset fields (sku, price) are generated; `name` is pinned.
        val req = CreateProduct.generate.request {
            body { name = Arb.constant("Wireless Mouse") }
        }
        req.body.name shouldBe "Wireless Mouse"
        req.body.sku.isNotEmpty() shouldBe true
    }

    test("a built request chains into a send with .call()") {
        // `generate.request { … }` materialises the typed request; `.call()` sends that exact
        // request and returns the contract-validated response variant.
        val response = CreateProduct.generate.request {
            body { name = Arb.constant("Wireless Mouse") }
        }.call()
        response.status shouldBe 201
    }

    test("response201 builds a random CreateProduct.Response201, pinning only what you set") {
        // Per-variant response builder: whole-value `body` setter, everything else generated.
        val pinned = Product(
            id = ProductId("00000000-0000-0000-0000-000000000000"),
            sku = "SKU-1",
            name = "Wireless Mouse",
            price = 9.99,
        )
        val response = CreateProduct.generate.response201 { body = Arb.constant(pinned) }
        response.status shouldBe 201
        response.body shouldBe pinned
    }

    test("GetProduct on an unknown id returns a 404 with a contract Error") {
        GetProduct.generate.call {
            path { id = Arb.constant(ProductId("00000000-0000-0000-0000-000000000000")) }
            expecting<GetProduct.Response404> { response ->
                response.body.code shouldBe 404L
            }
        }
    }

    test("a created campaign can be fetched back by its id") {
        val created = CreateCampaign.generate.call {
            body {
                name = Arb.constant("Spring Launch")
                discountPercentage = Arb.constant(15L)
                productIds = Arb.constant(emptyList<String>())
            }
            expecting<CreateCampaign.Response201>()
        }.body

        GetCampaign.generate.call {
            path { id = Arb.constant(created.id) }
            expecting<GetCampaign.Response200> { response ->
                response.body.id shouldBe created.id
                response.body.name shouldBe "Spring Launch"
                response.body.discountPercentage shouldBe 15L
            }
        }
    }
})

private val UUID_REGEX =
    Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

private fun String.shouldBeUuid() {
    check(UUID_REGEX.matches(this)) { "expected a UUID-shaped id, got '$this'" }
}
