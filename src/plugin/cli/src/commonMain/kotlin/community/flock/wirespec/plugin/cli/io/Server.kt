package community.flock.wirespec.plugin.cli.io

import community.flock.wirespec.compiler.core.parse.Endpoint
import kotlin.reflect.KFunction1

data class Request(
    val path:String
)

data class Response(
    val body:String
)
expect class Server(handle: (req:Request) -> Response) {
    fun start(port: Int)
}