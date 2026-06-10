package example

import community.flock.wirespec.generated.generator.*
import community.flock.wirespec.generated.model.*
import community.flock.wirespec.integration.kotest.KotestWirespec
import zio.*
import zio.test.*

object GeneratorSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("GeneratorSpec")(
    test("TodoDtoGenerator produces a TodoDto whose refined fields validate") {
      val gen = KotestWirespec.generator(seed = 1L)
      val todo: TodoDto = TodoDtoGenerator.generate(gen, List.empty)
      assertTrue(todo.validate().isEmpty)
    },
    test("UserGenerator returns a populated User") {
      val gen = KotestWirespec.generator(seed = 1L)
      val _: User = UserGenerator.generate(gen, List.empty)
      assertCompletes
    },
    test("Different seeds produce different TodoDtos") {
      val a = TodoDtoGenerator.generate(KotestWirespec.generator(seed = 1L), List.empty)
      val b = TodoDtoGenerator.generate(KotestWirespec.generator(seed = 2L), List.empty)
      assertTrue(a != b)
    },
  )
}
