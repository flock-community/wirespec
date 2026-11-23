package community.flock.wirespec.integration.kotest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import community.flock.wirespec.integration.jackson.v2.kotlin.WirespecSerialization
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
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.call
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.generate
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.mock
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.modify
import community.flock.wirespec.integration.kotest.kotlin.generated.kotest.send
import community.flock.wirespec.integration.kotest.kotlin.generated.model.NotFound
import community.flock.wirespec.integration.kotest.kotlin.generated.model.Role
import community.flock.wirespec.integration.kotest.kotlin.generated.model.Todo
import community.flock.wirespec.integration.kotest.kotlin.generated.model.TodoError
import community.flock.wirespec.integration.kotest.kotlin.generated.model.User
import community.flock.wirespec.integration.kotest.kotlin.generated.model.UserId
import community.flock.wirespec.integration.kotest.kotlin.generated.rpc.FindUsers
import community.flock.wirespec.integration.kotest.kotlin.generated.rpc.GetUser
import community.flock.wirespec.integration.kotest.runtime.WirespecSeed
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.RandomSource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.reflect.typeOf
import kotlin.test.Test

class ModifyDslTest {

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
    fun modifyStartsFromTheRequestCallSends() = runBlocking<Unit> {
        val transportation = RecordingTransportation(Wirespec.RawResponse(statusCode = 404, headers = emptyMap(), body = """{"reason":"gone"}""".encodeToByteArray()))

        withContext(endpointScope(transportation)) {
            getTodo42().modify {
                method shouldBe "GET"
                path shouldBe listOf("todos", "42")
                headers shouldBe mapOf("token" to listOf("secret"))
                body.shouldBeNull()
            }.call()
            getTodo42().call()
        }

        transportation.requests shouldBe List(2) { getTodo(id = "42", headers = mapOf("token" to listOf("secret"))) }
    }

    @Test
    fun aModifiedRequestIsSentAsIsAndItsResponseIsNotCheckedAgainstTheContract() = runBlocking<Unit> {
        val transportation = RecordingTransportation(Wirespec.RawResponse(statusCode = 401, headers = mapOf("reason" to listOf("token")), body = "missing token".encodeToByteArray()))

        withContext(endpointScope(transportation)) {
            val response = getTodo42().modify { headers -= "token" }.call()

            response.status shouldBe 401
            response.headers shouldBe mapOf("reason" to listOf("token"))
            response.body?.decodeToString() shouldBe "missing token"
            transportation.requests.single() shouldBe getTodo(id = "42", headers = emptyMap())

            shouldThrow<AssertionError> { getTodo42().call() }.message shouldContain "status 401 is not declared by the contract"
        }
    }

    @Test
    fun aModifiedBodyIsAnInputOfAnyType() = runBlocking<Unit> {
        val transportation = RecordingTransportation(Wirespec.RawResponse(statusCode = 400, headers = emptyMap(), body = null))

        withContext(endpointScope(transportation)) {
            getTodo42().modify {
                method = "PUT"
                body("name" to 42, "done" to "maybe")
            }.call()
            getTodo42().modify { body(Todo(id = "1", name = "Buy milk", done = false)) }.call()
            getTodo42().modify { body("not json".encodeToByteArray()) }.call()
        }

        transportation.requests.map { it.method } shouldBe listOf("PUT", "GET", "GET")
        transportation.requests.map { it.body?.decodeToString() } shouldBe listOf(
            """{"name":42,"done":"maybe"}""",
            """{"id":"1","name":"Buy milk","done":false}""",
            "not json",
        )
    }

    @Test
    fun aModifiedResponseIsMockedForTheRequestsThePredicateAccepts() = runBlocking<Unit> {
        val server = RecordingMockServer()

        withContext(mockScope(server)) {
            GetTodo.generate.response200 { body = Todo.generate { name("Buy milk") } }
                .modify {
                    status shouldBe 200
                    serialization.deserializeBody<Todo>(body as ByteArray, typeOf<Todo>()).name shouldBe "Buy milk"
                    status = 503
                    body(TodoError("maintenance"))
                }
                .mock { request -> request.path.id == "42" }
        }

        val stub = server.stubs.single()
        stub.method shouldBe "GET"
        stub.pathTemplate shouldBe "/todos/{id}"
        stub.response.statusCode shouldBe 503
        stub.response.body?.decodeToString() shouldBe """{"reason":"maintenance"}"""
        stub.matches(getTodo(id = "42", headers = mapOf("token" to listOf("secret")))) shouldBe true
        stub.matches(getTodo(id = "43", headers = mapOf("token" to listOf("secret")))) shouldBe false
        stub.matches(getTodo(id = "42", headers = emptyMap())) shouldBe false
    }

    @Test
    fun aModifiedMessageIsPublishedWithItsBody() = runBlocking<Unit> {
        val transport = RecordingChannelTransport()

        withContext(channelScope(transport)) {
            TodoEvents.generate.message { name("Buy milk") }.modify { }.send("todos", key = "t-1")
            TodoEvents.generate.message().modify { body("name" to 42) }.send("todos")
            TodoEvents.generate.message().modify { body("not json".encodeToByteArray()) }.send()
        }

        val (unchanged, typed, bytes) = transport.published
        unchanged.topic shouldBe "todos"
        unchanged.key shouldBe "t-1"
        serialization.deserializeBody<Todo>(unchanged.body, typeOf<Todo>()).name shouldBe "Buy milk"
        typed.body.decodeToString() shouldBe """{"name":42}"""
        bytes.topic shouldBe "TodoEvents"
        bytes.body.decodeToString() shouldBe "not json"
    }

    @Test
    fun aModifiedRpcRequestReachesTheServiceAsIs() = runBlocking<Unit> {
        val receivedIds = mutableListOf<UserId>()
        val receivedRequests = mutableListOf<FindUsersRequest>()
        val getUser = GetUser.Service { id -> GetUser.Error(NotFound("no such user")).also { receivedIds += id } }
        val findUsers = FindUsers.Service { role, limit -> emptyList<User>().also { receivedRequests += FindUsersRequest(role, limit) } }

        withContext(rpcScope(getUser, findUsers)) {
            GetUser.generate.request().modify { copy(id = UserId("not-a-user-id")) }.call() shouldBe GetUser.Error(NotFound("no such user"))
            FindUsers.generate.request { role(Role.ADMIN) }.modify { copy(limit = 0L) }.call()
        }

        receivedIds shouldBe listOf(UserId("not-a-user-id"))
        UserId("not-a-user-id").validate() shouldBe false
        receivedRequests shouldBe listOf(FindUsersRequest(role = Role.ADMIN, limit = 0L))
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

    private class Published(val topic: String, val key: String?, val body: ByteArray)

    private class RecordingChannelTransport : ChannelTransport {
        val published = mutableListOf<Published>()

        override suspend fun publish(topic: String, key: String?, body: ByteArray) {
            published += Published(topic, key, body)
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
