package example.adapter.http

import community.flock.wirespec.scala.Wirespec
import zio.*
import zio.http.*
import zio.http.codec.PathCodec

object ZIOHttpFromWirespec {

  private def fromZioRequest(request: Request): ZIO[Any, Throwable, Wirespec.RawRequest] =
    request.body.asArray.map { bodyBytes =>
      Wirespec.RawRequest(
        method = request.method.name,
        path = request.url.path.encode.stripPrefix("/").split("/").filter(_.nonEmpty).toList,
        queries = request.url.queryParams.map.view.mapValues(_.toList).toMap,
        headers = request.headers
          .groupMap(_.headerName)(_.renderedValue)
          .view
          .mapValues(_.toList)
          .toMap,
        body = if (bodyBytes.nonEmpty) Some(bodyBytes) else None
      )
    }

  private def toZioResponse(raw: Wirespec.RawResponse): Response = {
    val headerSeq = raw.headers.flatMap { case (k, vs) => vs.map(Header.Custom(k, _)) }.toSeq
    Response(
      status = Status.fromInt(raw.statusCode),
      headers = Headers(headerSeq *),
      body = raw.body.fold(Body.empty)(Body.fromArray)
    )
  }

  private def processRequest[R, Req <: Wirespec.Request[?], Res <: Wirespec.Response[?]](
    edge: Wirespec.ServerEdge[Req, Res],
    f: Req => ZIO[R, Throwable, Res],
    zioReq: Request
  ): ZIO[R, Throwable, Response] =
    for {
      rawReq <- fromZioRequest(zioReq)
      wirespecReq = edge.from(rawReq)
      wirespecRes <- f(wirespecReq)
      rawRes = edge.to(wirespecRes)
    } yield toZioResponse(rawRes)

  extension [Req <: Wirespec.Request[?], Res <: Wirespec.Response[?]](server: Wirespec.Server[Req, Res])
    def toRoute[R](
      f: Req => ZIO[R, Throwable, Res]
    ): Route[R & Wirespec.Serialization, Throwable] =
      val codec = server.pathSegments.foldLeft(PathCodec.empty: PathCodec[Unit]) {
        case (acc, Wirespec.Literal(v))     => acc / v
        case (acc, Wirespec.Param(name, _)) => acc / PathCodec.string(name).transform(_ => ())(_ => "")
      }
      (Method.fromString(server.method) / codec) -> handler { (req: Request) =>
        ZIO.service[Wirespec.Serialization].flatMap { serialization =>
          processRequest(server.server(serialization), f, req)
        }
      }
}
