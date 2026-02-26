package example

import community.flock.wirespec.generated.endpoint.*
import community.flock.wirespec.generated.model.*
import zio.*
import zio.http.*
import zio.test.*

object GuruClientSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("GuruClientSpec")(
    test("getMetrics returns parsed Metrics via HTTP") {
      val json = """{"numSpecs":3329,"numAPIs":2501,"numEndpoints":106448}"""
      for {
        _ <- TestClient.addRoutes(Routes(
          Method.GET / "v2" / "metrics.json" -> handler(Response.json(json))
        ))
        result <- GuruClient.getMetrics
      } yield {
        val r200 = result.asInstanceOf[GetMetrics.Response200]
        assertTrue(
          r200.body.numSpecs == 3329L,
          r200.body.numAPIs == 2501L,
          r200.body.numEndpoints == 106448L,
          r200.body.unreachable.isEmpty,
          r200.body.stars.isEmpty,
        )
      }
    }.provide(TestClient.layer, Scope.default),
    test("getMetrics deserializes optional fields") {
      val json =
        """{
          |"numSpecs":100,"numAPIs":50,"numEndpoints":500,
          |"unreachable":5,"stars":1000,
          |"thisWeek":{"added":10,"updated":20},
          |"numProviders":42
          |}""".stripMargin.replaceAll("\\n", "")
      for {
        _ <- TestClient.addRoutes(Routes(
          Method.GET / "v2" / "metrics.json" -> handler(Response.json(json))
        ))
        result <- GuruClient.getMetrics
      } yield {
        val r200 = result.asInstanceOf[GetMetrics.Response200]
        assertTrue(
          r200.body.numSpecs == 100L,
          r200.body.numAPIs == 50L,
          r200.body.numEndpoints == 500L,
          r200.body.unreachable == Some(5L),
          r200.body.stars == Some(1000L),
          r200.body.thisWeek.flatMap(_.added) == Some(10L),
          r200.body.numProviders == Some(42L),
        )
      }
    }.provide(TestClient.layer, Scope.default),
    test("getProviders returns parsed provider list via HTTP") {
      val json = """{"data":["amazonaws.com","googleapis.com","azure.com"]}"""
      for {
        _ <- TestClient.addRoutes(Routes(
          Method.GET / "v2" / "providers.json" -> handler(Response.json(json))
        ))
        result <- GuruClient.getProviders
      } yield {
        val r200 = result.asInstanceOf[GetProviders.Response200]
        assertTrue(
          r200.body.data.isDefined,
          r200.body.data.get.length == 3,
          r200.body.data.get.head == "amazonaws.com",
        )
      }
    }.provide(TestClient.layer, Scope.default),
    test("listAPIs returns parsed API map via HTTP") {
      val json = """{"test.api":{"added":"2024-01-01T00:00:00Z","preferred":"v1","versions":{}}}"""
      for {
        _ <- TestClient.addRoutes(Routes(
          Method.GET / "v2" / "list.json" -> handler(Response.json(json))
        ))
        result <- GuruClient.listAPIs
      } yield {
        val r200 = result.asInstanceOf[ListAPIs.Response200]
        assertTrue(
          r200.body.contains("test.api"),
          r200.body("test.api").preferred == "v1",
        )
      }
    }.provide(TestClient.layer, Scope.default),
  )
}
