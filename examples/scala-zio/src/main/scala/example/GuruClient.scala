package example

import community.flock.wirespec.scala.Wirespec
import community.flock.wirespec.generated.endpoint.*
import community.flock.wirespec.generated.model.*
import zio.*
import zio.http.{Client as ZClient, *}

object GuruClient {

  private val baseUrl = URL.decode("https://api.apis.guru/v2").toOption.get

  extension [Req <: Wirespec.Request[?], Res <: Wirespec.Response[?]](c: Wirespec.Client[Req, Res])
    def call(request: Req): ZIO[ZClient & Scope, Throwable, Res] = {
      val edge = c.client(CirceSerialization)
      val rawReq = edge.to(request)
      val url = baseUrl.copy(path = baseUrl.path ++ Path.decode("/" + rawReq.path.mkString("/")))
      val zioReq = Request(
        method = Method.fromString(rawReq.method),
        url = url,
        body = rawReq.body.map(b => Body.fromArray(b)).getOrElse(Body.empty),
      )
      for {
        response <- ZClient.request(zioReq)
        bodyBytes <- response.body.asArray
        rawRes = Wirespec.RawResponse(
          statusCode = response.status.code,
          headers = Map.empty,
          body = Some(bodyBytes)
        )
      } yield edge.from(rawRes)
    }
}
