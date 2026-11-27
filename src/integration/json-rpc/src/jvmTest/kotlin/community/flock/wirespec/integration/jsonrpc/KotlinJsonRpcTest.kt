package community.flock.wirespec.integration.jsonrpc

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import community.flock.wirespec.integration.jackson.v2.kotlin.WirespecSerialization
import community.flock.wirespec.integration.jsonrpc.kotlin.generated.JsonRpc
import community.flock.wirespec.integration.jsonrpc.kotlin.generated.JsonRpcClient
import community.flock.wirespec.integration.jsonrpc.kotlin.generated.JsonRpcException
import community.flock.wirespec.integration.jsonrpc.kotlin.generated.JsonRpcServer
import community.flock.wirespec.integration.jsonrpc.kotlin.generated.model.User
import community.flock.wirespec.integration.jsonrpc.kotlin.generated.model.UserNotFound
import community.flock.wirespec.integration.jsonrpc.kotlin.generated.rpc.Echo
import community.flock.wirespec.integration.jsonrpc.kotlin.generated.rpc.FindUsers
import community.flock.wirespec.integration.jsonrpc.kotlin.generated.rpc.GetUser
import community.flock.wirespec.integration.jsonrpc.kotlin.generated.rpc.Ping
import community.flock.wirespec.integration.jsonrpc.kotlin.generated.rpc.RenameUser
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class KotlinJsonRpcTest {

    private val users = mutableMapOf("1" to User(name = "Ada", age = 36))

    private val service = object : GetUser.Service, FindUsers.Service, RenameUser.Service, Ping.Service, Echo.Service {
        override suspend fun getUser(id: String): GetUser.Response = users[id]
            ?.let { GetUser.Result(value = it) }
            ?: GetUser.Error(value = UserNotFound(id = id))

        override suspend fun findUsers(name: String?, limit: Int?): List<User> = users.values
            .filter { name == null || it.name == name }
            .take(limit ?: Int.MAX_VALUE)

        override suspend fun renameUser(id: String, name: String): RenameUser.Response = users[id]
            ?.let { users[id] = it.copy(name = name) }
            ?.let { RenameUser.Result(value = Unit) }
            ?: RenameUser.Error(value = "No user $id")

        override suspend fun ping(): String = "pong \"quoted\" é"

        override suspend fun echo(user: User, tags: List<String>): User? = user.takeIf { "keep" in tags }
    }

    private val serialization = WirespecSerialization(jacksonObjectMapper())

    private val server = JsonRpcServer(serialization, service)

    private val sent = mutableListOf<String>()

    private val client = JsonRpcClient(
        serialization,
        Wirespec.Transportation { request -> sent.add(request.body!!.decodeToString()).let { server.handle(request) } },
    )

    private fun handle(body: String): Wirespec.RawResponse = runBlocking {
        server.handle(Wirespec.RawRequest(method = "POST", path = emptyList(), queries = emptyMap(), headers = emptyMap(), body = body.encodeToByteArray()))
    }

    private fun answer(body: String): String = handle(body).body!!.decodeToString()

    @Test
    fun `The client sends a request with the params under their Wirespec names`() = runBlocking<Unit> {
        client.findUsers(name = "Ada", limit = null)
        sent.single() shouldEqualJson """{"jsonrpc":"2.0","method":"findUsers","params":{"name":"Ada"},"id":1}"""
    }

    @Test
    fun `An rpc without fields is sent without params`() = runBlocking<Unit> {
        client.ping() shouldBe "pong \"quoted\" é"
        sent.single() shouldEqualJson """{"jsonrpc":"2.0","method":"ping","id":1}"""
    }

    @Test
    fun `The result of an rpc is read as its Result`() = runBlocking<Unit> {
        client.getUser(id = "1") shouldBe GetUser.Result(value = User(name = "Ada", age = 36))
    }

    @Test
    fun `The error of an rpc is an application defined error, with the error type as its message and the error as its data`() = runBlocking<Unit> {
        client.getUser(id = "2") shouldBe GetUser.Error(value = UserNotFound(id = "2"))
        answer("""{"jsonrpc":"2.0","method":"getUser","params":{"id":"2"},"id":7}""") shouldEqualJson
            """{"jsonrpc":"2.0","error":{"code":1,"message":"UserNotFound","data":{"id":"2"}},"id":7}"""
        answer("""{"jsonrpc":"2.0","method":"users/rename","params":{"id":"3","name":"Grace"},"id":8}""") shouldEqualJson
            """{"jsonrpc":"2.0","error":{"code":404,"message":"Error","data":"No user 3"},"id":8}"""
    }

    @Test
    fun `A method can be named with an annotation, and a unit result is null`() = runBlocking<Unit> {
        client.renameUser(id = "1", name = "Grace") shouldBe RenameUser.Result(value = Unit)
        client.renameUser(id = "3", name = "Grace") shouldBe RenameUser.Error(value = "No user 3")
        sent.first() shouldEqualJson """{"jsonrpc":"2.0","method":"users/rename","params":{"id":"1","name":"Grace"},"id":1}"""
        users.getValue("1").name shouldBe "Grace"
    }

    @Test
    fun `Lists and nullable results are written by the serialization`() = runBlocking<Unit> {
        client.findUsers(name = null, limit = 1) shouldBe listOf(User(name = "Ada", age = 36))
        client.echo(user = User(name = "Alan", age = null), tags = listOf("keep")) shouldBe User(name = "Alan", age = null)
        client.echo(user = User(name = "Alan", age = null), tags = emptyList()) shouldBe null
    }

    @Test
    fun `The server reads params by position`() {
        answer("""{"jsonrpc":"2.0","method":"getUser","params":["1"],"id":"a"}""") shouldEqualJson
            """{"jsonrpc":"2.0","result":{"name":"Ada","age":36},"id":"a"}"""
    }

    @Test
    fun `A batch is answered with an answer per request, and a notification is not answered`() {
        answer("""[{"jsonrpc":"2.0","method":"ping","id":1},{"jsonrpc":"2.0","method":"ping"},{"jsonrpc":"2.0","method":"nope","id":2}]""") shouldEqualJson
            """[{"jsonrpc":"2.0","result":"pong \"quoted\" é","id":1},{"jsonrpc":"2.0","error":{"code":-32601,"message":"Method not found"},"id":2}]"""
        handle("""{"jsonrpc":"2.0","method":"ping"}""").let {
            it.statusCode shouldBe 204
            it.body shouldBe null
        }
    }

    @Test
    fun `A request that breaks the protocol is answered with the error JSON-RPC defines`() {
        answer("""{"jsonrpc":"2.0","method":"getUser","params":{"id":3},"id":1}""") shouldEqualJson
            """{"jsonrpc":"2.0","error":{"code":-32602,"message":"Invalid params"},"id":1}"""
        answer("""{"jsonrpc":"2.0","method":"getUser","params":{},"id":1}""") shouldEqualJson
            """{"jsonrpc":"2.0","error":{"code":-32602,"message":"Invalid params"},"id":1}"""
        answer("""{"jsonrpc":"1.0","method":"ping","id":1}""") shouldEqualJson
            """{"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"},"id":1}"""
        answer("""{"jsonrpc":"2.0","method":"ping","id":1""") shouldEqualJson
            """{"jsonrpc":"2.0","error":{"code":-32700,"message":"Parse error"},"id":null}"""
        answer("""[]""") shouldEqualJson
            """{"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"},"id":null}"""
    }

    @Test
    fun `Any JSON the server cannot parse is a parse error, wherever it is`() {
        listOf("", "ping", """{"jsonrpc":"2.0","method":"ping","id":tru}""", """{"jsonrpc":"2.0","method":"ping","id":01}""", """{"jsonrpc":"2.0","method":"ping","id":1,}""", """{"jsonrpc":"2.0","method":"p\x"}""", "[".repeat(100_000))
            .forEach { body -> answer(body) shouldEqualJson """{"jsonrpc":"2.0","error":{"code":-32700,"message":"Parse error"},"id":null}""" }
    }

    @Test
    fun `An id must be a String, a Number or Null, or it is an invalid request with id null`() {
        answer("""{"jsonrpc":"2.0","method":"ping","id":{"a":1}}""") shouldEqualJson
            """{"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"},"id":null}"""
        answer("""{"jsonrpc":"2.0","method":"ping","id":true}""") shouldEqualJson
            """{"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"},"id":null}"""
        answer("""{"jsonrpc":"2.0","method":"ping","id":null}""") shouldEqualJson
            """{"jsonrpc":"2.0","result":"pong \"quoted\" é","id":null}"""
        answer("""{"jsonrpc":"2.0","method":"ping","id":-1.5e3}""") shouldEqualJson
            """{"jsonrpc":"2.0","result":"pong \"quoted\" é","id":-1.5e3}"""
    }

    @Test
    fun `The version is the String 2_0, however it is written`() {
        answer("""{"jsonrpc":"2\u002e0","method":"ping","id":1}""") shouldEqualJson
            """{"jsonrpc":"2.0","result":"pong \"quoted\" é","id":1}"""
        answer("""{"jsonrpc":2.0,"method":"ping","id":1}""") shouldEqualJson
            """{"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"},"id":1}"""
    }

    @Test
    fun `The client fails on an answer that is no JSON-RPC 2_0 response`() {
        listOf(
            """{"result":"pong","id":1}""",
            """{"jsonrpc":"2.0","result":"pong","error":{"code":1,"message":"Both"},"id":1}""",
            """{"jsonrpc":"2.0","result":"pong","id":2}""",
            """{"jsonrpc":"2.0","error":{"code":"1","message":"Code"},"id":1}""",
            """not json""",
        ).forEach { body ->
            val answering = JsonRpcClient(serialization) { Wirespec.RawResponse(statusCode = 200, headers = emptyMap(), body = body.encodeToByteArray()) }
            shouldThrow<IllegalStateException> { runBlocking { answering.ping() } }
        }
    }

    @Test
    fun `A service that throws is answered with an internal error`() {
        val failing = JsonRpcServer(
            serialization,
            object : GetUser.Service by service, FindUsers.Service by service, RenameUser.Service by service, Ping.Service, Echo.Service by service {
                override suspend fun ping(): String = error("Down")
            },
        )
        runBlocking { failing.handle(Wirespec.RawRequest("POST", emptyList(), emptyMap(), emptyMap(), """{"jsonrpc":"2.0","method":"ping","id":1}""".encodeToByteArray())) }
            .body!!.decodeToString() shouldEqualJson """{"jsonrpc":"2.0","error":{"code":-32603,"message":"Internal error"},"id":1}"""
    }

    @Test
    fun `The client throws an error object that is not the error of the rpc`() {
        val unknown = JsonRpcClient(serialization) { request -> server.handle(request.copy(body = request.body!!.decodeToString().replace("ping", "pong").encodeToByteArray())) }
        shouldThrow<JsonRpcException> { runBlocking { unknown.ping() } }.let {
            it.code shouldBe JsonRpc.METHOD_NOT_FOUND
            it.message shouldBe "Method not found"
            it.data shouldBe null
        }
        val failing = JsonRpcClient(serialization) {
            Wirespec.RawResponse(statusCode = 200, headers = emptyMap(), body = """{"jsonrpc":"2.0","error":{"code":-32000,"message":"Busy","data":{"retry":5}},"id":null}""".encodeToByteArray())
        }
        shouldThrow<JsonRpcException> { runBlocking { failing.getUser(id = "1") } }.let {
            it.code shouldBe -32000
            it.message shouldBe "Busy"
            it.data shouldBe """{"retry":5}"""
        }
    }

    @Test
    fun `Strings are escaped and unescaped`() {
        val text = "quote \" backslash \\ newline \n tab \t unicode ☃ emoji 😀 control \u0001"
        JsonRpc.decodeString(JsonRpc.encode(text)) shouldBe text
        JsonRpc.decodeString("\"\\u0041\\/\\b\"") shouldBe "A/\b"
    }
}
