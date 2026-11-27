package community.flock.wirespec.integration.jsonrpc

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import community.flock.wirespec.integration.jackson.v2.kotlin.WirespecSerialization
import community.flock.wirespec.integration.jsonrpc.sync.generated.JsonRpcClient
import community.flock.wirespec.integration.jsonrpc.sync.generated.JsonRpcServer
import community.flock.wirespec.integration.jsonrpc.sync.generated.model.User
import community.flock.wirespec.integration.jsonrpc.sync.generated.model.UserNotFound
import community.flock.wirespec.integration.jsonrpc.sync.generated.rpc.Echo
import community.flock.wirespec.integration.jsonrpc.sync.generated.rpc.FindUsers
import community.flock.wirespec.integration.jsonrpc.sync.generated.rpc.GetUser
import community.flock.wirespec.integration.jsonrpc.sync.generated.rpc.Ping
import community.flock.wirespec.integration.jsonrpc.sync.generated.rpc.RenameUser
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class SyncJsonRpcTest {

    private val handlingThread = mutableListOf<Thread>()

    private val service = object : GetUser.Service, FindUsers.Service, RenameUser.Service, Ping.Service, Echo.Service {
        override fun getUser(id: String): GetUser.Response = handlingThread.add(Thread.currentThread()).let { GetUser.Error(value = UserNotFound(id = id)) }

        override fun findUsers(name: String?, limit: Int?): List<User> = emptyList()

        override fun renameUser(id: String, name: String): RenameUser.Response = RenameUser.Result(value = Unit)

        override fun ping(): String = "pong"

        override fun echo(user: User, tags: List<String>): User? = null
    }

    private val serialization = WirespecSerialization(jacksonObjectMapper())

    private val server = JsonRpcServer(serialization, service)

    @Test
    fun `A synchronous server calls the services on the thread that handles the request`() {
        val client = JsonRpcClient(serialization) { request -> server.handle(request) }
        runBlocking { client.getUser("2") } shouldBe GetUser.Error(value = UserNotFound(id = "2"))
        handlingThread.single() shouldBe Thread.currentThread()
    }
}
