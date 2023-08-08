package community.flock.wirespec.compiler.core

import arrow.core.curried
import community.flock.wirespec.compiler.common.TestLogger
import community.flock.wirespec.compiler.common.assertValid
import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitter
import kotlin.test.Test

class CompileEnumTest {

    private val logger = TestLogger

    private val compiler = compile(
        """
        enum MyAwesomeEnum {
          ONE, Two
        }
        """.trimIndent()
    )

    @Test
    fun testEnumKotlin() {
        val kotlin = """
            package community.flock.wirespec.generated
            
            enum class MyAwesomeEnum (val label: String){
              ONE("ONE"),
              Two("Two")
            }
            
        """.trimIndent()

        compiler(KotlinEmitter(logger = logger)).assertValid(kotlin)
    }

    @Test
    fun testEnumJava() {
        val java = """
            package community.flock.wirespec.generated;
            
            public enum MyAwesomeEnum {
              ONE("ONE"),
              Two("Two");
              public final String label;
              MyAwesomeEnum(String label) {
                this.label = label;
              }
            }

        """.trimIndent()

        compiler(JavaEmitter(logger = logger)).assertValid(java)
    }

    @Test
    fun testEnumScala() {
        val scala = """
            package community.flock.wirespec.generated
            
            sealed abstract class MyAwesomeEnum(val label: String)
            object MyAwesomeEnum {
              final case object ONE extends MyAwesomeEnum(label = "ONE")
              final case object TWO extends MyAwesomeEnum(label = "Two")
            }

        """.trimIndent()

        compiler(ScalaEmitter(logger = logger)).assertValid(scala)
    }

    @Test
    fun testEnumTypeScript() {
        val ts = """
            type MyAwesomeEnum = "ONE" | "Two"

        """.trimIndent()

        compiler(TypeScriptEmitter(logger = logger)).assertValid(ts)
    }

    @Test
    fun testEnumWirespec() {
        val wirespec = """
            enum MyAwesomeEnum {
              ONE, Two
            }
        
        """.trimIndent()

        compiler(WirespecEmitter(logger = logger)).assertValid(wirespec)
    }

    private fun compile(source: String) = { emitter: Emitter ->
        Wirespec.compile(source)(logger)(emitter)
            .map { it.first().second }
            .onLeft(::println)
    }
}
