package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class CompileEnumTest {

    private val logger = noLogger

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
              Two("Two");
            
              override fun toString(): String {
                return label
              }
            }
            
        """.trimIndent()

        compiler(KotlinEmitter(logger = logger)) shouldBeRight kotlin
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
              @Override
              public String toString() {
                return label;
              }
            }

        """.trimIndent()

        compiler(JavaEmitter(logger = logger)) shouldBeRight java
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

        compiler(ScalaEmitter(logger = logger)) shouldBeRight scala
    }

    @Test
    fun testEnumTypeScript() {
        val ts = """
            type MyAwesomeEnum = "ONE" | "Two"

        """.trimIndent()

        compiler(TypeScriptEmitter(logger = logger)) shouldBeRight ts
    }

    @Test
    fun testEnumWirespec() {
        val wirespec = """
            enum MyAwesomeEnum {
              ONE, Two
            }
        
        """.trimIndent()

        compiler(WirespecEmitter(logger = logger)) shouldBeRight wirespec
    }

    private fun compile(source: String) = { emitter: AbstractEmitter ->
        Wirespec.compile(source)(logger)(emitter)
            .map { it.first().result }
            .onLeft(::println)
    }
}
