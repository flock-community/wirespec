package example

import community.flock.wirespec.scala.Wirespec
import community.flock.wirespec.generated.endpoint.*
import community.flock.wirespec.generated.model.*
import zio.*
import zio.http.*

object GuruClient {

  private val baseUrl = URL.decode("https://api.apis.guru/v2").toOption.get

  private def call[Req, Res](
    toRawRequest: (Wirespec.Serializer, Req) => Wirespec.RawRequest,
    fromRawResponse: (Wirespec.Deserializer, Wirespec.RawResponse) => Res,
    request: Req,
    serialization: Wirespec.Serialization
  ): ZIO[Client & Scope, Throwable, Res] = {
    val rawReq = toRawRequest(serialization, request)
    val url = baseUrl.copy(path = baseUrl.path ++ Path.decode("/" + rawReq.path.mkString("/")))
    val zioReq = Request(
      method = Method.fromString(rawReq.method),
      url = url,
      body = rawReq.body.map(b => Body.fromArray(b)).getOrElse(Body.empty),
    )
    for {
      response <- Client.request(zioReq)
      bodyBytes <- response.body.asArray
      rawRes = Wirespec.RawResponse(
        statusCode = response.status.code,
        headers = Map.empty,
        body = Some(bodyBytes)
      )
    } yield fromRawResponse(serialization, rawRes)
  }

  def getMetrics: ZIO[Client & Scope, Throwable, GetMetrics.Response[?]] =
    call(GetMetrics.toRawRequest, GetMetrics.fromRawResponse, GetMetrics.Request, CirceSerialization)

  def getProviders: ZIO[Client & Scope, Throwable, GetProviders.Response[?]] =
    call(GetProviders.toRawRequest, GetProviders.fromRawResponse, GetProviders.Request, CirceSerialization)

  def listAPIs: ZIO[Client & Scope, Throwable, ListAPIs.Response[?]] =
    call(ListAPIs.toRawRequest, ListAPIs.fromRawResponse, ListAPIs.Request, CirceSerialization)
}
