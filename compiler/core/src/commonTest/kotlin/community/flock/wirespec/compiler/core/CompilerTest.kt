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

    @Test
    fun testRefinedType() {
        val source = """
            refined Name /^[a-zA-Z]{1,50}$/g
        """.trimIndent()

        val out = """
            package community.flock.wirespec.generated
            
            data class Name(val value: String)
            fun Name.validate() = Regex("^[a-zA-Z]{1,50}$").find(value)
            
        """.trimIndent()

        Wirespec.compile(source)(logger)(KotlinEmitter(logger = logger))
            .map { it.first().second }
            .onLeft(::println)
            .assertValid(out)
    }
}
