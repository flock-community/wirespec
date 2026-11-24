package community.flock.wirespec.integration.jsonrpc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import community.flock.wirespec.integration.jackson.v2.java.WirespecSerialization
import community.flock.wirespec.integration.jsonrpc.java.generated.JsonRpc
import community.flock.wirespec.integration.jsonrpc.java.generated.JsonRpcClient
import community.flock.wirespec.integration.jsonrpc.java.generated.JsonRpcException
import community.flock.wirespec.integration.jsonrpc.java.generated.JsonRpcServer
import community.flock.wirespec.integration.jsonrpc.java.generated.model.User
import community.flock.wirespec.integration.jsonrpc.java.generated.model.UserNotFound
import community.flock.wirespec.integration.jsonrpc.java.generated.rpc.Echo
import community.flock.wirespec.integration.jsonrpc.java.generated.rpc.FindUsers
import community.flock.wirespec.integration.jsonrpc.java.generated.rpc.GetUser
import community.flock.wirespec.integration.jsonrpc.java.generated.rpc.Ping
import community.flock.wirespec.integration.jsonrpc.java.generated.rpc.RenameUser
import community.flock.wirespec.java.Wirespec
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.test.Test

class JavaJsonRpcTest {

    private val ada = User("Ada", Optional.of(36L))

    private interface Users :
        GetUser.Service,
        FindUsers.Service,
        RenameUser.Service,
        Ping.Service,
        Echo.Service

    private val service = object : Users {
        override fun getUser(id: String): CompletableFuture<GetUser.Response> = CompletableFuture.completedFuture(
            if (id == "1") GetUser.Result(ada) else GetUser.Error(UserNotFound(id)),
        )

        override fun findUsers(name: Optional<String>, limit: Optional<Int>): CompletableFuture<List<User>> = CompletableFuture.supplyAsync {
            listOf(User(name.orElse("anyone"), limit.map { it.toLong() }))
        }

        override fun renameUser(id: String, name: String): CompletableFuture<RenameUser.Response> = CompletableFuture.completedFuture(
            if (id == "1") RenameUser.Result(null) else RenameUser.Error("No user $id"),
        )

        override fun ping(): CompletableFuture<String> = CompletableFuture.completedFuture("pong")

        override fun echo(user: User, tags: List<String>): CompletableFuture<Optional<User>> = CompletableFuture.completedFuture(Optional.of(user).filter { "keep" in tags })
    }

    private val serialization = WirespecSerialization(ObjectMapper().registerModule(Jdk8Module()))

    private val server = JsonRpcServer(serialization, service)

    private val sent = mutableListOf<String>()

    private val client = JsonRpcClient(serialization) { request -> sent.add(String(request.body().get())).let { server.handle(request) } }

    private fun answer(body: String): String = server.handle(Wirespec.RawRequest("POST", emptyList(), emptyMap(), emptyMap(), Optional.of(body.toByteArray())))
        .join().body().map { String(it) }.orElse("")

    @Test
    fun `The client sends a request and reads the result`() {
        client.getUser("1").join() shouldBe GetUser.Result(ada)
        sent.single() shouldEqualJson """{"jsonrpc":"2.0","method":"getUser","params":{"id":"1"},"id":1}"""
    }

    @Test
    fun `The error of an rpc is read as its Error`() {
        client.getUser("2").join() shouldBe GetUser.Error(UserNotFound("2"))
        client.renameUser("3", "Grace").join() shouldBe RenameUser.Error("No user 3")
        client.renameUser("1", "Grace").join() shouldBe RenameUser.Result(null)
    }

    @Test
    fun `Optional params are left out, and optional results are empty`() {
        client.findUsers(Optional.empty(), Optional.of(2)).join() shouldBe listOf(User("anyone", Optional.of(2L)))
        sent.single() shouldEqualJson """{"jsonrpc":"2.0","method":"findUsers","params":{"limit":2},"id":1}"""
        client.echo(ada, listOf("keep")).join() shouldBe Optional.of(ada)
        client.echo(ada, emptyList()).join() shouldBe Optional.empty()
        client.ping().join() shouldBe "pong"
    }

    @Test
    fun `The server answers a batch and the errors JSON-RPC defines`() {
        answer("""[{"jsonrpc":"2.0","method":"findUsers","params":["Bob"],"id":1},{"jsonrpc":"2.0","method":"ping"},{"jsonrpc":"2.0","method":"getUser","params":{},"id":2}]""") shouldEqualJson
            """[{"jsonrpc":"2.0","result":[{"name":"Bob","age":null}],"id":1},{"jsonrpc":"2.0","error":{"code":-32602,"message":"Invalid params"},"id":2}]"""
        answer("""{"jsonrpc":"2.0","method":"nope","id":"x"}""") shouldEqualJson
            """{"jsonrpc":"2.0","error":{"code":-32601,"message":"Method not found"},"id":"x"}"""
        answer("""{"jsonrpc":"2.0","method":"getUser","params":{"id":"2"},"id":3}""") shouldEqualJson
            """{"jsonrpc":"2.0","error":{"code":-32000,"message":"Server error","data":{"id":"2"}},"id":3}"""
        answer("""nope""") shouldEqualJson """{"jsonrpc":"2.0","error":{"code":-32600,"message":"Invalid Request"},"id":null}"""
        server.handle(Wirespec.RawRequest("POST", emptyList(), emptyMap(), emptyMap(), Optional.of("""{"jsonrpc":"2.0","method":"ping"}""".toByteArray()))).join().statusCode() shouldBe 204
    }

    @Test
    fun `A service that fails is answered with an internal error`() {
        val failing = JsonRpcServer(
            serialization,
            object : Users by service {
                override fun ping(): CompletableFuture<String> = CompletableFuture.failedFuture(IllegalStateException("Down"))
            },
        )
        failing.handle(Wirespec.RawRequest("POST", emptyList(), emptyMap(), emptyMap(), Optional.of("""{"jsonrpc":"2.0","method":"ping","id":1}""".toByteArray())))
            .join().body().map { String(it) }.get() shouldEqualJson """{"jsonrpc":"2.0","error":{"code":-32603,"message":"Internal error"},"id":1}"""
    }

    @Test
    fun `The client fails with an error object that is not the error of the rpc`() {
        val unknown = JsonRpcClient(serialization) { request -> server.handle(Wirespec.RawRequest("POST", emptyList(), emptyMap(), emptyMap(), Optional.of(String(request.body().get()).replace("ping", "pong").toByteArray()))) }
        shouldThrow<CompletionException> { unknown.ping().join() }.cause.let {
            it.shouldBeInstanceOf<JsonRpcException>()
            it.code shouldBe JsonRpc.METHOD_NOT_FOUND
            it.message shouldBe "Method not found"
        }
    }
}
