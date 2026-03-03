package example

import community.flock.wirespec.generated.endpoint.*
import community.flock.wirespec.generated.model.*
import zio.*
import zio.http.*
import zio.test.*

object GuruServerSpec extends ZIOSpecDefault {

  private val handler = GuruHandlerLive()
  private val routes = GuruServer.routes(handler)

  private def get(path: String): ZIO[Any, Response, Response] =
    routes.runZIO(Request.get(URL.decode(path).toOption.get))

  private def bodyString(response: Response): ZIO[Any, Throwable, String] =
    response.body.asString

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("GuruServerSpec")(
    suite("literal path routes")(
      test("GET /metrics.json returns 200 with metrics") {
        for {
          response <- get("/metrics.json")
          body <- bodyString(response)
        } yield assertTrue(
          response.status == Status.Ok,
          body.contains("\"numSpecs\":42"),
          body.contains("\"numProviders\":5"),
        )
      },
      test("GET /providers.json returns 200 with providers") {
        for {
          response <- get("/providers.json")
          body <- bodyString(response)
        } yield assertTrue(
          response.status == Status.Ok,
          body.contains("googleapis.com"),
          body.contains("azure.com"),
          body.contains("amazonaws.com"),
        )
      },
      test("GET /list.json returns 200 with API list") {
        for {
          response <- get("/list.json")
          body <- bodyString(response)
        } yield assertTrue(
          response.status == Status.Ok,
          body == "{}",
        )
      },
    ),
    suite("parameterized path routes")(
      test("GET /{provider}/services.json routes to GetServices") {
        for {
          response <- get("/googleapis.com/services.json")
          body <- bodyString(response)
        } yield assertTrue(
          response.status == Status.Ok,
          body.contains("\"data\":[]"),
        )
      },
      test("GET /specs/{provider}/{api}.json routes to GetAPI") {
        for {
          response <- get("/specs/googleapis.com/v1.json")
          body <- bodyString(response)
        } yield assertTrue(
          response.status == Status.Ok,
          body.contains("\"preferred\":\"v1\""),
          body.contains("\"added\":\"2023-01-01\""),
        )
      },
      test("GET /specs/{provider}/{service}/{api}.json routes to GetServiceAPI") {
        for {
          response <- get("/specs/googleapis.com/compute/v1.json")
          body <- bodyString(response)
        } yield assertTrue(
          response.status == Status.Ok,
          body.contains("\"preferred\":\"v1\""),
        )
      },
      test("GET /{provider}.json routes to GetProvider") {
        for {
          response <- get("/googleapis.com.json")
          body <- bodyString(response)
        } yield assertTrue(
          response.status == Status.Ok,
          body == "{}",
        )
      },
    ),
    suite("route specificity")(
      test("/{provider}/services.json matches GetServices, not GetProvider") {
        for {
          response <- get("/googleapis.com/services.json")
          body <- bodyString(response)
        } yield assertTrue(
          response.status == Status.Ok,
          body.contains("\"data\""),
        )
      },
      test("/specs/{provider}/{service}/{api}.json matches GetServiceAPI over GetAPI") {
        for {
          r3 <- get("/specs/googleapis.com/compute/v1.json")
          r2 <- get("/specs/googleapis.com/v1.json")
          b3 <- bodyString(r3)
          b2 <- bodyString(r2)
        } yield assertTrue(
          r3.status == Status.Ok,
          r2.status == Status.Ok,
          b3 == b2,
        )
      },
    ),
    suite("404 handling")(
      test("unmatched path returns 404") {
        for {
          response <- get("/nonexistent/path/that/does/not/match/anything")
        } yield assertTrue(response.status == Status.NotFound)
      },
      test("POST to GET-only route returns 404") {
        for {
          response <- routes.runZIO(Request(method = Method.POST, url = URL.decode("/metrics.json").toOption.get))
        } yield assertTrue(response.status == Status.NotFound)
      },
    ),
  )
}
