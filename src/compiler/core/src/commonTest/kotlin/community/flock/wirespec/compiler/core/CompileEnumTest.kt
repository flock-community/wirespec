package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.AbstractEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test
import kotlin.test.assertEquals

class CompileEnumTest {

    private val logger = noLogger

    private val compiler = compile(
        """
        enum MyAwesomeEnum {
          ONE, Two, THREE_MORE
        }
        """.trimIndent()
    )

    @Test
    fun testEnumKotlin() {
        val kotlin = """
            |package community.flock.wirespec.generated
            |
            |import community.flock.wirespec.Wirespec
            |
            |enum class MyAwesomeEnum (val label: String): Wirespec.Enum {
            |  ONE("ONE"),
            |  Two("Two"),
            |  THREE_MORE("THREE_MORE");
            |  override fun toString(): String {
            |    return label
            |  }
            |}
            |
        """.trimMargin()

        assertEquals(kotlin, compiler(KotlinEmitter(logger = logger)).getOrNull())

        compiler(KotlinEmitter(logger = logger)) shouldBeRight kotlin
    }

    @Test
    fun testEnumJava() {
        val java = """
            |package community.flock.wirespec.generated;
            |
            |import community.flock.wirespec.Wirespec;
            |
            |public enum MyAwesomeEnum implements Wirespec.Enum {
            |  ONE("ONE"),
            |  Two("Two"),
            |  THREE_MORE("THREE_MORE");
            |  public final String label;
            |  MyAwesomeEnum(String label) {
            |    this.label = label;
            |  }
            |  @Override
            |  public String toString() {
            |    return label;
            |  }
            |}
            |
        """.trimMargin()

        assertEquals(java, compiler(JavaEmitter(logger = logger)).getOrNull())

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
              final case object THREE_MORE extends MyAwesomeEnum(label = "THREE_MORE")
            }

        """.trimIndent()

        compiler(ScalaEmitter(logger = logger)) shouldBeRight scala
    }

    @Test
    fun testEnumTypeScript() {
        val ts = """
            export type MyAwesomeEnum = "ONE" | "Two" | "THREE_MORE"

        """.trimIndent()

        compiler(TypeScriptEmitter(logger = logger)) shouldBeRight ts
    }

    @Test
    fun testEnumWirespec() {
        val wirespec = """
            enum MyAwesomeEnum {
              ONE, Two, THREE_MORE
            }
        
        """.trimIndent()

        compiler(WirespecEmitter(logger = logger)) shouldBeRight wirespec
    }

    private fun compile(source: String) = { emitter: Emitter ->
        Wirespec.compile(source)(logger)(emitter)
            .map { it.first().result }
            .onLeft(::println)
    }
}
