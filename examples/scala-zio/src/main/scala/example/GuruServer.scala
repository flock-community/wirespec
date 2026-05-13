package example

import community.flock.wirespec.generated.endpoint.*
import community.flock.wirespec.generated.model.*
import zio.*
import zio.http.*

trait GuruHandler extends GetMetrics.Handler[Task]
  with GetProviders.Handler[Task]
  with GetProvider.Handler[Task]
  with GetAPI.Handler[Task]
  with GetServices.Handler[Task]
  with GetServiceAPI.Handler[Task]
  with ListAPIs.Handler[Task]

class GuruHandlerLive extends GuruHandler {

  override def getMetrics(request: GetMetrics.Request.type): Task[GetMetrics.Response[?]] =
    ZIO.succeed(new GetMetrics.Response200(Metrics(
      numSpecs = 42,
      numAPIs = 10,
      numEndpoints = 120,
      unreachable = None,
      invalid = None,
      unofficial = None,
      fixes = None,
      fixedPct = None,
      datasets = None,
      stars = None,
      issues = None,
      thisWeek = None,
      numDrivers = None,
      numProviders = Some(5)
    )))

  override def getProviders(request: GetProviders.Request.type): Task[GetProviders.Response[?]] =
    ZIO.succeed(new GetProviders.Response200(GetProviders200ResponseBody(
      data = Some(List("googleapis.com", "azure.com", "amazonaws.com"))
    )))

  override def getProvider(request: GetProvider.Request.type): Task[GetProvider.Response[?]] =
    ZIO.succeed(new GetProvider.Response200(Map.empty[String, API]))

  override def getAPI(request: GetAPI.Request): Task[GetAPI.Response[?]] =
    ZIO.succeed(new GetAPI.Response200(API(
      added = "2023-01-01",
      preferred = "v1",
      versions = Map.empty
    )))

  override def getServices(request: GetServices.Request): Task[GetServices.Response[?]] =
    ZIO.succeed(new GetServices.Response200(GetServices200ResponseBody(
      data = Some(List.empty)
    )))

  override def getServiceAPI(request: GetServiceAPI.Request): Task[GetServiceAPI.Response[?]] =
    ZIO.succeed(new GetServiceAPI.Response200(API(
      added = "2023-01-01",
      preferred = "v1",
      versions = Map.empty
    )))

  override def listAPIs(request: ListAPIs.Request.type): Task[ListAPIs.Response[?]] =
    ZIO.succeed(new ListAPIs.Response200(Map.empty[String, API]))
}

object GuruServer extends ZIOAppDefault {

  def routes(h: GuruHandler): Routes[Any, Response] =
    WirespecRouter(CirceSerialization)
      .route(GetMetrics.Server)(h.getMetrics)
      .route(GetProviders.Server)(h.getProviders)
      .route(ListAPIs.Server)(h.listAPIs)
      .route(GetServiceAPI.Server)(h.getServiceAPI)
      .route(GetAPI.Server)(h.getAPI)
      .route(GetServices.Server)(h.getServices)
      .route(GetProvider.Server)(h.getProvider)
      .toRoutes

  override val run: ZIO[Any, Any, Any] = {
    val h = GuruHandlerLive()
    for {
      _ <- ZIO.logInfo("Starting Guru API server on port 8080")
      _ <- Server.serve(routes(h))
    } yield ()
  }.provide(Server.defaultWithPort(8080))
}
