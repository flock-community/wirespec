package community.flock.wirespec.integration.kotest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import community.flock.wirespec.integration.jackson.v2.kotlin.WirespecSerialization
import community.flock.wirespec.integration.kotest.dsl.RawMessage
import community.flock.wirespec.integration.kotest.dsl.call
import community.flock.wirespec.integration.kotest.dsl.send
import community.flock.wirespec.integration.kotest.extension.ChannelTransport
import community.flock.wirespec.integration.kotest.extension.MockServer
import community.flock.wirespec.integration.kotest.extension.MockStub
import community.flock.wirespec.integration.kotest.extension.WirespecChannelContext
import community.flock.wirespec.integration.kotest.extension.WirespecEndpointContext
import community.flock.wirespec.integration.kotest.extension.WirespecMockContext
import community.flock.wirespec.integration.kotest.extension.WirespecRpcContext
import community.flock.wirespec.integration.kotest.kotlin.generated.channel.TodoEvents
import community.flock.wirespec.integration.kotest.kotlin.generated.endpoint.GetTodo
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.FindUsersRequest
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.GetUserRequest
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.call
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.generate
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.mock
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.raw
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.send
import community.flock.wirespec.integration.kotest.kotlin.generated.model.NotFound
import community.flock.wirespec.integration.kotest.kotlin.generated.model.Role
import community.flock.wirespec.integration.kotest.kotlin.generated.model.Todo
import community.flock.wirespec.integration.kotest.kotlin.generated.model.User
import community.flock.wirespec.integration.kotest.kotlin.generated.model.UserId
import community.flock.wirespec.integration.kotest.kotlin.generated.rpc.FindUsers
import community.flock.wirespec.integration.kotest.kotlin.generated.rpc.GetUser
import community.flock.wirespec.integration.kotest.runtime.WirespecSeed
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.property.RandomSource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.reflect.typeOf
import kotlin.test.Test

class RawDslTest {

    private val serialization = WirespecSerialization(jacksonObjectMapper())

    private fun endpointScope(transportation: Wirespec.Transportation) = WirespecEndpointContext(transportation, serialization) + WirespecSeed(RandomSource.seeded(1L))

    private fun mockScope(server: MockServer) = WirespecMockContext(server, serialization) + WirespecSeed(RandomSource.seeded(1L))

    private fun channelScope(transport: ChannelTransport) = WirespecChannelContext(transport, serialization) + WirespecSeed(RandomSource.seeded(1L))

    private fun rpcScope(vararg services: Any) = WirespecRpcContext(*services) + WirespecSeed(RandomSource.seeded(1L))

    private suspend fun getTodo42() = GetTodo.generate.request {
        path { id("42") }
        header { token("secret") }
    }

    @Test
    fun rawIsTheRequestCallSends() = runBlocking<Unit> {
        val transportation = RecordingTransportation(Wirespec.RawResponse(statusCode = 404, headers = emptyMap(), body = """{"reason":"gone"}""".encodeToByteArray()))

        withContext(endpointScope(transportation)) {
            val raw = getTodo42().raw()
            raw shouldBe getTodo(id = "42", headers = mapOf("token" to listOf("secret")))

            getTodo42().call()
            transportation.requests.single() shouldBe raw
        }
    }

    @Test
    fun aRawRequestIsSentAsIsAndItsResponseIsNotCheckedAgainstTheContract() = runBlocking<Unit> {
        val unauthorized = Wirespec.RawResponse(statusCode = 401, headers = emptyMap(), body = "missing token".encodeToByteArray())
        val transportation = RecordingTransportation(unauthorized)

        withContext(endpointScope(transportation)) {
            val withoutToken = getTodo42().raw().let { it.copy(headers = it.headers - "token") }

            withoutToken.call() shouldBeSameInstanceAs unauthorized
            transportation.requests.single() shouldBe withoutToken

            shouldThrow<AssertionError> { getTodo42().call() }.message shouldContain "status 401 is not declared by the contract"
        }
    }

    @Test
    fun rawIsTheResponseMockServes() = runBlocking<Unit> {
        val server = RecordingMockServer()

        withContext(mockScope(server)) {
            val raw = GetTodo.generate.response200 { body = Todo.generate { name("Buy milk") } }.raw()

            raw.statusCode shouldBe 200
            serialization.deserializeBody<Todo>(raw.body!!, typeOf<Todo>()).name shouldBe "Buy milk"
        }
        server.stubs shouldBe emptyList()
    }

    @Test
    fun aRawResponseMocksTheEndpointForTheRequestsThePredicateAccepts() = runBlocking<Unit> {
        val server = RecordingMockServer()
        val unavailable = Wirespec.RawResponse(statusCode = 503, headers = emptyMap(), body = null)

        withContext(mockScope(server)) {
            unavailable.mock(GetTodo) { request -> request.path.id == "42" }
        }

        val stub = server.stubs.single()
        stub.method shouldBe "GET"
        stub.pathTemplate shouldBe "/todos/{id}"
        stub.response shouldBeSameInstanceAs unavailable
        stub.matches(getTodo(id = "42", headers = mapOf("token" to listOf("secret")))) shouldBe true
        stub.matches(getTodo(id = "43", headers = mapOf("token" to listOf("secret")))) shouldBe false
        stub.matches(getTodo(id = "42", headers = emptyMap())) shouldBe false
    }

    @Test
    fun rawIsTheMessageSendPublishes() = runBlocking<Unit> {
        val transport = RecordingChannelTransport()
        val message = TodoEvents.generate.message { name("Buy milk") }

        val raw = withContext(channelScope(transport)) { message.raw("todos", key = "t-1") }
        transport.published shouldBe emptyList()
        withContext(channelScope(transport)) { message.send("todos", key = "t-1") }

        val sent = transport.published.single()
        sent.topic shouldBe "todos"
        sent.key shouldBe "t-1"
        sent.body.decodeToString() shouldBe raw.body.decodeToString()
        serialization.deserializeBody<Todo>(raw.body, typeOf<Todo>()).name shouldBe "Buy milk"
    }

    @Test
    fun aRawMessageWithoutATopicGoesToTheChannelName() = runBlocking<Unit> {
        withContext(channelScope(RecordingChannelTransport())) {
            TodoEvents.generate.message().raw().topic shouldBe "TodoEvents"
        }
    }

    @Test
    fun aRawMessageIsPublishedAsIs() = runBlocking<Unit> {
        val transport = RecordingChannelTransport()
        val notATodo = RawMessage(topic = "todos", key = null, body = """{"name":42}""".encodeToByteArray())

        withContext(channelScope(transport)) { notATodo.send() }

        val sent = transport.published.single()
        sent.topic shouldBe "todos"
        sent.key shouldBe null
        sent.body shouldBeSameInstanceAs notATodo.body
    }

    @Test
    fun rawIsTheRpcRequestCallHandsToTheService() = runBlocking<Unit> {
        val received = mutableListOf<FindUsersRequest>()
        val findUsers = FindUsers.Service { role, limit -> emptyList<User>().also { received += FindUsersRequest(role, limit) } }
        val request = FindUsers.generate.request { role(Role.ADMIN) }

        val raw = withContext(rpcScope(findUsers)) { request.raw() }
        received shouldBe emptyList()
        withContext(rpcScope(findUsers)) { request.call() }

        received shouldBe listOf(raw)
    }

    @Test
    fun aRawRpcRequestOutsideTheContractReachesTheServiceAsIs() = runBlocking<Unit> {
        val receivedIds = mutableListOf<UserId>()
        val receivedLimits = mutableListOf<Long>()
        val getUser = GetUser.Service { id -> GetUser.Error(NotFound("no such user")).also { receivedIds += id } }
        val findUsers = FindUsers.Service { _, limit -> emptyList<User>().also { receivedLimits += limit } }

        withContext(rpcScope(getUser, findUsers)) {
            GetUserRequest(id = UserId("not-a-user-id")).call() shouldBe GetUser.Error(NotFound("no such user"))
            FindUsers.generate.request().raw().copy(limit = 0L).call()
        }

        receivedIds shouldBe listOf(UserId("not-a-user-id"))
        UserId("not-a-user-id").validate() shouldBe false
        receivedLimits shouldBe listOf(0L)
    }

    private fun getTodo(id: String, headers: Map<String, List<String>>) = Wirespec.RawRequest(
        method = "GET",
        path = listOf("todos", id),
        queries = emptyMap(),
        headers = headers,
        body = null,
    )

    private class RecordingTransportation(private val response: Wirespec.RawResponse) : Wirespec.Transportation {
        val requests = mutableListOf<Wirespec.RawRequest>()

        override suspend fun transport(request: Wirespec.RawRequest): Wirespec.RawResponse = response.also { requests += request }
    }

    private class RecordingChannelTransport : ChannelTransport {
        val published = mutableListOf<RawMessage>()

        override suspend fun publish(topic: String, key: String?, body: ByteArray) {
            published += RawMessage(topic, key, body)
        }
    }

    private class RecordingMockServer : MockServer {
        val stubs = mutableListOf<MockStub>()

        override fun stub(stub: MockStub) {
            stubs += stub
        }

        override fun reset() = stubs.clear()
    }
}
