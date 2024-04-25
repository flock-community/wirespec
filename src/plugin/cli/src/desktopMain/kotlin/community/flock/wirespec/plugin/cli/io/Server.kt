package community.flock.wirespec.plugin.cli.io

import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

actual object Server {
    actual fun start(port: Int) {
        embeddedServer(CIO, port = port) {
            routing {
                get("/") {
                    call.respondText("Hello, wirespec")
                }
            }
        }.start(wait = true)
    }
}