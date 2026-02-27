package example

import community.flock.wirespec.scala.Wirespec
import zio.*
import zio.http.*

object ZIOHttpFromWirespec {

  private def fromZioRequest(request: Request): ZIO[Any, Throwable, Wirespec.RawRequest] =
    request.body.asArray.map { bodyBytes =>
      Wirespec.RawRequest(
        method = request.method.name,
        path = request.url.path.encode.stripPrefix("/").split("/").filter(_.nonEmpty).toList,
        queries = request.url.queryParams.map.view.mapValues(_.toList).toMap,
        headers = request.headers.map(h => h.headerName -> List(h.renderedValue)).toMap,
        body = if (bodyBytes.nonEmpty) Some(bodyBytes) else None
      )
    }

  private def toZioResponse(raw: Wirespec.RawResponse): Response = {
    val headerSeq = raw.headers.flatMap { case (k, vs) => vs.map(Header.Custom(k, _)) }.toSeq
    Response(
      status = Status.fromInt(raw.statusCode),
      headers = Headers(headerSeq*),
      body = raw.body.fold(Body.empty)(Body.fromArray)
    )
  }

  def handle[R, Req <: Wirespec.Request[?], Res <: Wirespec.Response[?]](
    serialization: Wirespec.Serialization,
    server: Wirespec.Server[Req, Res]
  )(
    f: Req => ZIO[R, Throwable, Res]
  ): Handler[R, Throwable, Request, Response] = {
    val edge = server.server(serialization)
    handler { (zioReq: Request) =>
      for {
        rawReq      <- fromZioRequest(zioReq)
        wirespecReq  = edge.from(rawReq)
        wirespecRes <- f(wirespecReq)
        rawRes       = edge.to(wirespecRes)
      } yield toZioResponse(rawRes)
    }
  }
}
