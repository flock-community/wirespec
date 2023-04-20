package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.common.TestLogger
import community.flock.wirespec.compiler.common.assertInvalid
import community.flock.wirespec.compiler.common.assertValid
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import kotlin.test.Test

class CompilerTest {

    private val logger = TestLogger

    @Test
    fun testCompileKotlin() {

        val source = """
            type Bla {
              foo: String,
              bar: String
            }

        """.trimIndent()

        val out = """
            package community.flock.wirespec.generated

            data class Bla(
              val foo: String,
              val bar: String
            )

        """.trimIndent()

        Wirespec.compile(source)(logger)(KotlinEmitter(logger = logger))
            .map { it.first().second }
            .assertValid(out)
    }

    @Test
    fun testCompileKotlinMissingColumnInBody() {

        val source = """
            type Bla {
              foo: String
              bar: String
            }

        """.trimIndent()

        Wirespec.compile(source)(logger)(KotlinEmitter(logger = logger))
            .assertInvalid("RightCurly expected, not: CustomValue at line 3 and position 3")
    }
}
