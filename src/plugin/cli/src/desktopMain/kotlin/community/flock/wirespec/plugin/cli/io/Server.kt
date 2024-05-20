package community.flock.wirespec.plugin.cli.io

import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

actual class Server actual constructor(val handle: (req:Request) -> Response) {
    actual fun start(port: Int) {
        embeddedServer(CIO, port = port) {
            routing {
                route("/{...}"){
                    handle {
                        val req = Request(call.request.path())
                        val res = handle(req)
                        call.respond(res.body)
                    }
                }
            }
        }.start(wait = true)
    }
}