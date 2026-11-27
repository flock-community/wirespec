package community.flock.wirespec.example.jsonrpc

import community.flock.wirespec.generated.JsonRpcServer
import community.flock.wirespec.kotlin.Wirespec
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.toMap

fun main() {
    embeddedServer(Netty, port = 8080) { jsonRpc(JsonRpcServer(JacksonSerialization, Users())) }.start(wait = true)
}

/** Serves every rpc at `POST /rpc`: the generated server reads the request, calls the service, and writes the response. */
fun Application.jsonRpc(server: JsonRpcServer<*>) {
    routing {
        post("/rpc") {
            call.respondRaw(
                server.handle(
                    Wirespec.RawRequest(
                        method = "POST",
                        path = listOf("rpc"),
                        queries = emptyMap(),
                        headers = call.request.headers.toMap(),
                        body = call.receive<ByteArray>(),
                    ),
                ),
            )
        }
    }
}

private suspend fun ApplicationCall.respondRaw(response: Wirespec.RawResponse) = when (val body = response.body) {
    null -> respond(HttpStatusCode.fromValue(response.statusCode))
    else -> respondBytes(body, ContentType.Application.Json, HttpStatusCode.fromValue(response.statusCode))
}
