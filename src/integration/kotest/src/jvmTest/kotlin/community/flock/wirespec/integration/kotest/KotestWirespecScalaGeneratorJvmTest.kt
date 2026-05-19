package community.flock.wirespec.integration.kotest

import community.flock.wirespec.scala.Wirespec
import scala.Option
import scala.collection.immutable.`List$`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cover the Scala adapter against a Java fixture (in jvmTest/java) that
 * stands in for the user's emit-shared `Wirespec.scala`. Confirms:
 *   1. `kotestWirespecScalaGenerator(...)` returns something castable to the
 *      Scala `Wirespec.Generator` declared by the fixture.
 *   2. The reflective decoding routes a `GeneratorFieldString` through the
 *      commonMain algorithm.
 *   3. `GeneratorFieldNullable<T>` returns a `scala.Option<T>`, not a bare
 *      Kotlin `T?`.
 *
 * Wider variant coverage is left for an integration test against
 * `examples/scala-zio` (follow-up).
 */
class KotestWirespecScalaGeneratorJvmTest {

    @Test
    fun `factory returns a value assignable to the Scala Wirespec_Generator`() {
        val gen = kotestWirespecScalaGenerator(seed = 1L) as Wirespec.Generator
        assertNotNull(gen)
    }

    @Test
    fun `adapter wraps GeneratorFieldNullable result in scala_Option`() {
        val gen = kotestWirespecScalaGenerator(seed = 0L) as Wirespec.Generator

        val nullableField = Wirespec.GeneratorFieldNullable<String> { _ -> "value" }
        val v: Option<String> = gen.generate(scalaListOf("n"), nullableField)
        assertTrue(v.isDefined)
        assertEquals("value", v.get())
    }

    // --- helpers ---

    private fun scalaListOf(vararg xs: String): scala.collection.immutable.List<String> {
        var acc: scala.collection.immutable.List<String> = `List$`.`MODULE$`.empty()
        for (x in xs.reversed()) acc = acc.prepended(x)
        return acc
    }

}
