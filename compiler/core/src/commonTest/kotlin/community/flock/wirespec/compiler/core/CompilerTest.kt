import community.flock.wirespec.compiler.core.WireSpec
import community.flock.wirespec.compiler.core.assertLeft
import community.flock.wirespec.compiler.core.assertRight
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.utils.Logger
import kotlin.test.Test

class CompilerTest {

    private val source = """
        type Bla {
          foo: String,
          bar: String
        }
        
    """.trimIndent()

    @Test
    fun `testCompileKotlin`() {

        val out = """
            data class Bla(
              val foo: String,
              val bar: String
            )
          
            
        """.trimIndent()

        val logger: Logger = object : Logger(enableLogging = false) {}
        val res = WireSpec.compile(source)(logger)(KotlinEmitter(logger))
        assertRight(out, res)
    }

    @Test
    fun `testCompileKotlinMissingColumnInBody`() {
        val logger: Logger = object : Logger(enableLogging = false) {}
        val res = WireSpec.compile(source)(logger)(KotlinEmitter(logger))
        assertLeft("RightCurly expected, not: CustomValue at line 3 and position 3", res)

    }
}