package community.flock.wirespec.integration.kotest

import community.flock.wirespec.scala.Wirespec
import org.junit.Assert.{assertEquals, assertTrue}
import org.junit.Test

/** Compile-checks the Scala facade against the Kotlin API and smoke-tests it
  * through the Scala `Wirespec` runtime from `:src:integration:wirespec`.
  */
class KotestWirespecFacadeTest {

  private val stringField = Wirespec.GeneratorFieldString(Option.empty, List.empty)

  @Test def generatorProducesAStringWithoutCasts(): Unit = {
    val gen: Wirespec.Generator = KotestWirespec.generator(seed = 1L)
    val v: String = gen.generate(List("id"), stringField)
    assertTrue(s"expected a non-empty string, got '$v'", v.nonEmpty)
  }

  @Test def configurePinsAPathOverride(): Unit = {
    val gen = KotestWirespec.generator(
      seed = 1L,
      configure = b => b.registerPath(Array("u", "id"), "FIXED"),
    )
    val v: String = gen.generate(List("u", "id"), stringField)
    assertEquals("FIXED", v)
  }
}
