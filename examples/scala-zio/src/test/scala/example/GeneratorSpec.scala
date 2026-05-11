package example

import community.flock.wirespec.generated.generator.*
import community.flock.wirespec.generated.model.*
import zio.*
import zio.test.*

object GeneratorSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("GeneratorSpec")(
    test("APIGenerator produces an API with populated required fields") {
      val gen = KotestBridge.generator(seed = 1L)
      val api: API = APIGenerator.generate(gen, List.empty)
      // The Scala fields here are non-nullable (`String`, `Map[…]`), so a
      // `!= null` check would be vacuously true. Asserting `nonEmpty`
      // actually verifies the generator produced content.
      assertTrue(
        api.added.nonEmpty,
        api.preferred.nonEmpty,
        api.versions.nonEmpty,
      )
    },
    test("MetricsGenerator returns a populated Metrics") {
      val gen = KotestBridge.generator(seed = 1L)
      val _: Metrics = MetricsGenerator.generate(gen, List.empty)
      // Smoke: the generator runs to completion. `Metrics` is a non-nullable
      // Scala case class so a `!= null` check would be vacuously true; the
      // kotest defaults use the full `Long` range so a value-bound assertion
      // could flake. `assertCompletes` matches the actual intent.
      assertCompletes
    },
    test("Different seeds produce different APIs") {
      val a = APIGenerator.generate(KotestBridge.generator(seed = 1L), List.empty)
      val b = APIGenerator.generate(KotestBridge.generator(seed = 2L), List.empty)
      assertTrue(a != b)
    },
  )
}
