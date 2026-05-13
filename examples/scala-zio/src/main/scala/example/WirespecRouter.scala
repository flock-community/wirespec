package example

import community.flock.wirespec.scala.Wirespec
import zio.*
import zio.http.*

class WirespecRouter(serialization: Wirespec.Serialization) {

  private case class RegisteredRoute(
    server: Wirespec.Server[? <: Wirespec.Request[?], ? <: Wirespec.Response[?]],
    handler: Any => Task[Any],
    segments: List[SegmentMatcher],
    method: String
  )

  private sealed trait SegmentMatcher
  private case class LiteralMatcher(value: String) extends SegmentMatcher
  private case class WildcardMatcher(name: String) extends SegmentMatcher
  private case class SuffixMatcher(name: String, suffix: String) extends SegmentMatcher

  private var registeredRoutes: List[RegisteredRoute] = Nil

  def route[Req <: Wirespec.Request[?], Res <: Wirespec.Response[?]](
    server: Wirespec.Server[Req, Res]
  )(handle: Req => Task[Res]): WirespecRouter = {
    val segments = parseTemplate(server.pathTemplate)
    registeredRoutes = registeredRoutes :+ RegisteredRoute(
      server.asInstanceOf[Wirespec.Server[? <: Wirespec.Request[?], ? <: Wirespec.Response[?]]],
      handle.asInstanceOf[Any => Task[Any]],
      segments,
      server.method
    )
    this
  }

  private def parseTemplate(template: String): List[SegmentMatcher] = {
    template.stripPrefix("/").split("/").toList.map { segment =>
      if (segment.startsWith("{") && segment.endsWith("}")) {
        WildcardMatcher(segment.drop(1).dropRight(1))
      } else if (segment.contains("{") && segment.contains("}")) {
        val start = segment.indexOf('{')
        val end = segment.indexOf('}')
        val name = segment.substring(start + 1, end)
        val suffix = segment.substring(end + 1)
        SuffixMatcher(name, suffix)
      } else {
        LiteralMatcher(segment)
      }
    }
  }

  private def matchPath(matchers: List[SegmentMatcher], path: List[String]): Boolean = {
    if (matchers.length != path.length) return false
    matchers.zip(path).forall {
      case (LiteralMatcher(expected), actual) => expected == actual
      case (WildcardMatcher(_), _) => true
      case (SuffixMatcher(_, suffix), actual) => actual.endsWith(suffix)
    }
  }

  private def specificity(segments: List[SegmentMatcher]): (Int, Int, Int) = {
    val literals = segments.count(_.isInstanceOf[LiteralMatcher])
    val suffixed = segments.count(_.isInstanceOf[SuffixMatcher])
    val length = segments.length
    (-literals, -suffixed, -length)
  }

  private def pathSegments(req: Request): List[String] =
    req.url.path.segments.toList

  def toRoutes: Routes[Any, Response] = {
    val sorted = registeredRoutes.sortBy(r => specificity(r.segments))

    Routes(
      Method.ANY / trailing -> Handler.fromFunctionZIO[(Path, Request)] { case (_, req: Request) =>
        val path = pathSegments(req)
        val method = req.method.name
        sorted.find(r => r.method == method && matchPath(r.segments, path)) match {
          case Some(r) =>
            val edge = r.server.asInstanceOf[Wirespec.Server[Wirespec.Request[Any], Wirespec.Response[Any]]]
              .server(serialization)
            (for {
              bodyBytes <- req.body.asArray
              rawReq = toRawRequest(req, bodyBytes, path)
              typedReq = edge.from(rawReq)
              typedRes <- r.handler(typedReq)
              rawRes = edge.to(typedRes.asInstanceOf[Wirespec.Response[Any]])
            } yield toZioResponse(rawRes)).mapError(e => Response.internalServerError(e.getMessage))
          case None =>
            ZIO.succeed(Response.status(Status.NotFound))
        }
      }
    )
  }

  private def toRawRequest(req: Request, bodyBytes: Array[Byte], path: List[String]): Wirespec.RawRequest =
    Wirespec.RawRequest(
      method = req.method.name,
      path = path,
      queries = req.url.queryParams.map.view.mapValues(_.toList).toMap,
      headers = req.headers.toList.map(h => h.headerName -> List(h.renderedValue)).toMap,
      body = if (bodyBytes.isEmpty) None else Some(bodyBytes)
    )

  private def toZioResponse(rawRes: Wirespec.RawResponse): Response = {
    val headers = Headers(rawRes.headers.flatMap { case (k, vs) => vs.map(v => Header.Custom(k, v)) }.toList)
    Response(
      status = Status.fromInt(rawRes.statusCode),
      headers = headers ++ Headers(Header.ContentType(MediaType.application.json)),
      body = rawRes.body.map(b => Body.fromArray(b)).getOrElse(Body.empty)
    )
  }
}
