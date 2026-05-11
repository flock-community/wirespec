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
  )
}
