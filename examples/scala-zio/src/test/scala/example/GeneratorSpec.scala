package example

import community.flock.wirespec.generated.generator.*
import community.flock.wirespec.generated.model.*
import zio.*
import zio.test.*

object GeneratorSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("GeneratorSpec")(
    test("TodoDtoGenerator produces a TodoDto whose refined fields validate") {
      val gen = KotestBridge.generator(seed = 1L)
      val todo: TodoDto = TodoDtoGenerator.generate(gen, List.empty)
      // `validate()` returns the fields whose refined value is invalid.
      assertTrue(todo.validate().isEmpty)
    },
    test("UserGenerator returns a populated User") {
      val gen = KotestBridge.generator(seed = 1L)
      val _: User = UserGenerator.generate(gen, List.empty)
      assertCompletes
    },
    test("Different seeds produce different TodoDtos") {
      val a = TodoDtoGenerator.generate(KotestBridge.generator(seed = 1L), List.empty)
      val b = TodoDtoGenerator.generate(KotestBridge.generator(seed = 2L), List.empty)
      assertTrue(a != b)
    },
  )
}
