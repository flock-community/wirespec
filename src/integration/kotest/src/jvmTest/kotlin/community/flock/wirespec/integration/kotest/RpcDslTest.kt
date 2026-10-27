package community.flock.wirespec.integration.kotest

import community.flock.wirespec.integration.kotest.dsl.draw
import community.flock.wirespec.integration.kotest.extension.WirespecRpcContext
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.call
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.generate
import community.flock.wirespec.integration.kotest.kotlin.generated.model.NotFound
import community.flock.wirespec.integration.kotest.kotlin.generated.model.Role
import community.flock.wirespec.integration.kotest.kotlin.generated.model.UserId
import community.flock.wirespec.integration.kotest.kotlin.generated.rpc.FindUsers
import community.flock.wirespec.integration.kotest.kotlin.generated.rpc.GetUser
import community.flock.wirespec.integration.kotest.kotlin.generated.rpc.Ping
import community.flock.wirespec.integration.kotest.runtime.WirespecSeed
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.property.RandomSource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.test.Test

class RpcDslTest {

    private val rs = RandomSource.seeded(42L)

    private fun rpcScope(vararg services: Any) = WirespecRpcContext(*services) + WirespecSeed(RandomSource.seeded(1L))

    @Test
    fun requestGeneratesEveryParameterWithinTheContract() {
        GetUser.generate.request().draw(rs).id.value shouldMatch "^u-[0-9]{3}$"
        repeat(20) { FindUsers.generate.request().draw(rs).limit shouldBeInRange 1L..50L }
    }

    @Test
    fun requestPinsBuilderValuesAndWrapsRefinedParameters() {
        GetUser.generate.request { id("u-042") }.draw(rs).id shouldBe UserId("u-042")

        val request = FindUsers.generate.request {
            role(Role.ADMIN)
            limit(5L)
        }.draw(rs)
        request.role shouldBe Role.ADMIN
        request.limit shouldBe 5L
    }

    @Test
    fun resultPinsRecordFieldsAtAnyDepthAndGeneratesTheRest() {
        val result = GetUser.generate.result {
            name("Alice")
            addressBlock { street("Main Street") }
        }.draw(rs)

        result.value.name shouldBe "Alice"
        result.value.address.street shouldBe "Main Street"
        result.value.id.value shouldMatch "^u-[0-9]{3}$"
    }

    @Test
    fun listResultPinsEveryElement() {
        val users = FindUsers.generate.result { role(Role.MEMBER) }.draw(rs)

        users.shouldNotBeEmpty()
        users.forEach { it.role shouldBe Role.MEMBER }
    }

    @Test
    fun errorAndPrimitiveResultsAreGenerated() {
        GetUser.generate.error { reason("gone") }.draw(rs) shouldBe GetUser.Error(NotFound("gone"))
        Ping.generate.result().draw(rs).shouldNotBeEmpty()
    }

    @Test
    fun sameSeedDrawsTheSameRequest() {
        GetUser.generate.request().draw(RandomSource.seeded(7L)) shouldBe GetUser.generate.request().draw(RandomSource.seeded(7L))
    }

    @Test
    fun callHandsTheDrawnRequestToTheRegisteredService() = runBlocking<Unit> {
        val canned = GetUser.generate.result { name("Alice") }.draw(rs)
        val received = mutableListOf<UserId>()
        val getUser = GetUser.Service { id ->
            received += id
            canned
        }

        withContext(rpcScope(getUser, Ping.Service { "pong" })) {
            GetUser.generate.request { id("u-007") }.call() shouldBe canned
            Ping.generate.request().call() shouldBe "pong"
        }
        received shouldBe listOf(UserId("u-007"))
    }

    @Test
    fun callNamesTheServiceNoExtensionProvides() = runBlocking<Unit> {
        val failure = shouldThrow<IllegalStateException> {
            withContext(rpcScope()) { Ping.generate.request().call() }
        }
        failure.message shouldContain "Ping.Service"
    }
}
