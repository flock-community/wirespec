package example

import community.flock.wirespec.generated.generator.*
import community.flock.wirespec.generated.model.*
import zio.*
import zio.test.*

object GeneratorSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("GeneratorSpec")(
    test("APIGenerator produces non-null required fields") {
      val gen = KotestBridge.generator(seed = 1L)
      val api: API = APIGenerator.generate(gen, List.empty)
      assertTrue(
        api.added != null,
        api.preferred != null,
        api.versions != null,
      )
    },
  )
}
