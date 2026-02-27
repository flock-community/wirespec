package example

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
        headers = request.headers.map(h => h.headerName -> List(h.renderedValue)).toMap,
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

  private def buildRoute[R, Req <: Wirespec.Request[?], Res <: Wirespec.Response[?]](
    method: Method,
    segments: List[String],
    edge: Wirespec.ServerEdge[Req, Res],
    f: Req => ZIO[R, Throwable, Res]
  ): Route[R, Throwable] = {
    val varNames = segments.collect { case s if s.startsWith("{") && s.endsWith("}") => s.drop(1).dropRight(1) }
    varNames.size match {
      case 0 =>
        val codec = segments.foldLeft(PathCodec.empty: PathCodec[Unit]) { (acc, s) => acc / s }
        (method / codec) -> handler { (req: Request) => processRequest(edge, f, req) }

      case 1 =>
        val varName = varNames.head
        val varIdx = segments.indexWhere(s => s.startsWith("{") && s.endsWith("}"))
        val preCodec = segments.take(varIdx).foldLeft(PathCodec.empty: PathCodec[Unit]) { (acc, s) => acc / s }
        val midCodec: PathCodec[String] = preCodec / PathCodec.string(varName)
        val fullCodec: PathCodec[String] = segments.drop(varIdx + 1)
          .foldLeft(midCodec) { (acc, s) => acc / s }
        (method / fullCodec) -> handler { (_: String, req: Request) => processRequest(edge, f, req) }

      case n =>
        throw new UnsupportedOperationException(s"Path templates with $n path variables are not supported")
    }
  }

  def handle[R, Req <: Wirespec.Request[?], Res <: Wirespec.Response[?]](
    serialization: Wirespec.Serialization,
    server: Wirespec.Server[Req, Res]
  )(
    f: Req => ZIO[R, Throwable, Res]
  ): Handler[R, Throwable, Request, Response] = {
    val edge = server.server(serialization)
    handler { (zioReq: Request) => processRequest(edge, f, zioReq) }
  }

  extension [Req <: Wirespec.Request[?], Res <: Wirespec.Response[?]](server: Wirespec.Server[Req, Res])
    def toRoute[R](
      serialization: Wirespec.Serialization
    )(
      f: Req => ZIO[R, Throwable, Res]
    ): Route[R, Throwable] =
      val edge = server.server(serialization)
      val method = Method.fromString(server.method)
      val segments = server.pathTemplate.split("/").filter(_.nonEmpty).toList
      buildRoute(method, segments, edge, f)
}
