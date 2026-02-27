package example

import community.flock.wirespec.scala.Wirespec
import zio.http.*
import zio.http.codec.PathCodec

object ZIOHttpFromWirespec {
  extension [Req, Res](server: Wirespec.Server[Req, Res])
    def zioPath: RoutePattern[Res] = Method.fromString(server.method) / PathCodec.path(server.pathTemplate)

  extension [Req, Res](response: Wirespec.Response[Res])
    def toZio: Response =
      Response(
        status = Status.fromInt(response.status), 
        headers = response.headers.asInstanceOf[Response.Headers],
        body = response.body.asInstanceOf[String]
      )
}
