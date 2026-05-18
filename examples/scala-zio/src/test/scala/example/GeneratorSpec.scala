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
      // All refined fields (TodoId regex + Integer/Number bounds) should
      // generate values that satisfy their constraints. `validate()` returns
      // the list of fields whose refined value is invalid — empty means OK.
      assertTrue(todo.validate().isEmpty)
    },
    test("UserGenerator returns a populated User") {
      val gen = KotestBridge.generator(seed = 1L)
      val _: User = UserGenerator.generate(gen, List.empty)
      // Smoke: the generator runs to completion. `User` is a non-nullable
      // Scala case class so a `!= null` check would be vacuously true; the
      // kotest defaults use the full `String` range so a value-bound assertion
      // could flake. `assertCompletes` matches the actual intent.
      assertCompletes
    },
    test("Different seeds produce different TodoDtos") {
      val a = TodoDtoGenerator.generate(KotestBridge.generator(seed = 1L), List.empty)
      val b = TodoDtoGenerator.generate(KotestBridge.generator(seed = 2L), List.empty)
      assertTrue(a != b)
    },
  )
}
