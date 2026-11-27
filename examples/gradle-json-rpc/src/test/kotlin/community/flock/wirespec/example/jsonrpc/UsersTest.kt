package community.flock.wirespec.example.jsonrpc

import community.flock.wirespec.generated.JsonRpcClient
import community.flock.wirespec.generated.JsonRpcException
import community.flock.wirespec.generated.JsonRpcServer
import community.flock.wirespec.generated.model.User
import community.flock.wirespec.generated.model.UserNotFound
import community.flock.wirespec.generated.rpc.GetUser
import community.flock.wirespec.generated.rpc.RenameUser
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UsersTest {

    private fun ApplicationTestBuilder.users(): JsonRpcClient {
        application { jsonRpc(JsonRpcServer(JacksonSerialization, Users())) }
        return JsonRpcClient(JacksonSerialization, KtorTransportation(client, "/rpc"))
    }

    @Test
    fun theClientCallsTheServerOverHttp() = testApplication {
        val users = users()
        val ada = users.createUser(name = "Ada", email = "ada@example.com")
        val grace = users.createUser(name = "Grace", email = null)

        assertEquals(User(id = "1", name = "Ada", email = "ada@example.com"), ada)
        assertEquals(GetUser.Result(value = ada), users.getUser(id = ada.id))
        assertEquals(listOf(ada, grace), users.findUsers(name = null))
        assertEquals(listOf(grace), users.findUsers(name = "grace"))
        assertEquals(RenameUser.Result(value = grace.copy(name = "Grace Hopper")), users.renameUser(id = grace.id, name = "Grace Hopper"))
    }

    @Test
    fun theErrorOfAnRpcIsAValue() = testApplication {
        val users = users()

        assertEquals(GetUser.Error(value = UserNotFound(id = "42")), users.getUser(id = "42"))
        assertEquals(RenameUser.Error(value = UserNotFound(id = "42")), users.renameUser(id = "42", name = "Nobody"))
    }

    @Test
    fun anyJsonRpcClientCanCallTheServer() = testApplication {
        users()

        val batch = client.post("/rpc") {
            contentType(ContentType.Application.Json)
            setBody("""[{"jsonrpc":"2.0","method":"createUser","params":["Ada"],"id":1},{"jsonrpc":"2.0","method":"getUser","params":{"id":"2"},"id":2}]""")
        }
        assertEquals(
            """[{"jsonrpc":"2.0","result":{"id":"1","name":"Ada","email":null},"id":1},{"jsonrpc":"2.0","error":{"code":1,"message":"UserNotFound","data":{"id":"2"}},"id":2}]""",
            batch.bodyAsText(),
        )

        val notification = client.post("/rpc") {
            contentType(ContentType.Application.Json)
            setBody("""{"jsonrpc":"2.0","method":"createUser","params":{"name":"Grace"}}""")
        }
        assertEquals(HttpStatusCode.NoContent, notification.status)
    }

    @Test
    fun anErrorObjectThatIsNotTheErrorOfTheRpcFailsTheCall() = testApplication {
        users()
        routing {
            // A server that knows no method of the contract.
            post("/legacy") { call.respondText("""{"jsonrpc":"2.0","error":{"code":-32601,"message":"Method not found"},"id":1}""", ContentType.Application.Json) }
        }
        val legacy = JsonRpcClient(JacksonSerialization, KtorTransportation(client, "/legacy"))

        val error = assertFailsWith<JsonRpcException> { legacy.findUsers(name = null) }
        assertEquals(-32601, error.code)
        assertEquals("Method not found", error.message)
    }
}
