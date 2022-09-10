package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.util.assertLeft
import community.flock.wirespec.compiler.util.assertRight
import community.flock.wirespec.compiler.utils.Logger
import kotlin.test.Test

class CompilerTest {

    private val logger: Logger = object : Logger(enableLogging = false) {}

    @Test
    fun testCompileKotlin() {

        val source = """
            type Bla {
              foo: String,
              bar: String
            }
            
        """.trimIndent()

        val out = """
            data class Bla(
              val foo: String,
              val bar: String
            )
          
            
        """.trimIndent()

        val res = WireSpec.compile(source)(logger)(KotlinEmitter(logger))
        assertRight(out, res)
    }

    @Test
    fun testCompileKotlinMissingColumnInBody() {

        val source = """
            type Bla {
              foo: String
              bar: String
            }
            
        """.trimIndent()

        val res = WireSpec.compile(source)(logger)(KotlinEmitter(logger))
        assertLeft("RightCurly expected, not: CustomValue at line 3 and position 3", res)

    }
}
