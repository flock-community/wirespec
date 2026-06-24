package community.flock.wirespec.examples.kotest

import community.flock.wirespec.examples.kotest.generated.endpoint.CreateCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.CreateProduct
import community.flock.wirespec.examples.kotest.generated.endpoint.GetCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.GetProduct
import community.flock.wirespec.examples.kotest.generated.kotest.call
import community.flock.wirespec.examples.kotest.generated.model.ProductId
import community.flock.wirespec.integration.kotest.WirespecExtension
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant

/**
 * Endpoint scenarios driven by the generated `<Endpoint>.call { … }` DSL. Each call
 * builds its request body/path with kotest `Arb`s (unset fields are generated from
 * the contract), sends it over real HTTP to the running app, and validates the typed
 * response variant against the Wirespec contract.
 *
 * The transport is supplied by the shared `CampaignTestEnvironment` via
 * `ScenarioContextProvider`; this spec only mounts the ambient with `@ApplyExtension`.
 */
@ApplyExtension(WirespecExtension::class)
class CampaignEndpointScenarioTest : FunSpec({
    test("CreateProduct returns a 201 echoing the generated body") {
        CreateProduct.call {
            body = { name = Arb.constant("Wireless Mouse") }
            expecting<CreateProduct.Response201> { response ->
                response.body.name shouldBe "Wireless Mouse"
                response.body.id.value.shouldBeUuid()
            }
        }
    }

    test("GetProduct on an unknown id returns a 404 with a contract Error") {
        GetProduct.call {
            path = { id = Arb.constant(ProductId("00000000-0000-0000-0000-000000000000")) }
            expecting<GetProduct.Response404> { response ->
                response.body.code shouldBe 404L
            }
        }
    }

    test("a created campaign can be fetched back by its id") {
        val created = CreateCampaign.call {
            body = {
                name = Arb.constant("Spring Launch")
                discountPercentage = Arb.constant(15L)
                productIds = Arb.constant(emptyList<String>())
            }
            expecting<CreateCampaign.Response201>()
        }.body

        GetCampaign.call {
            path = { id = Arb.constant(created.id) }
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
