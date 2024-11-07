package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class CompileEnumTest {

    private val compiler = """
        |enum MyAwesomeEnum {
        |  ONE, Two, THREE_MORE
        |}
    """.trimMargin().let(::compile)

    @Test
    fun kotlin() {
        val kotlin = """
            |package community.flock.wirespec.generated
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |enum class MyAwesomeEnum (override val label: String): Wirespec.Enum {
            |  ONE("ONE"),
            |  Two("Two"),
            |  THREE_MORE("THREE_MORE");
            |  override fun toString(): String {
            |    return label
            |  }
            |}
            |
        """.trimMargin()

        compiler(KotlinEmitter()) shouldBeRight kotlin
    }

    @Test
    fun java() {
        val java = """
            |package community.flock.wirespec.generated;
            |
            |import community.flock.wirespec.java.Wirespec;
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
            |  @Override
            |  public String getLabel() {
            |    return label;
            |  }
            |}
            |
        """.trimMargin()

        compiler(JavaEmitter()) shouldBeRight java
    }

    @Test
    fun scala() {
        val scala = """
            |package community.flock.wirespec.generated
            |
            |import community.flock.wirespec.scala.Wirespec
            |
            |sealed abstract class MyAwesomeEnum(val label: String)
            |object MyAwesomeEnum {
            |  final case object ONE extends MyAwesomeEnum(label = "ONE")
            |  final case object TWO extends MyAwesomeEnum(label = "Two")
            |  final case object THREE_MORE extends MyAwesomeEnum(label = "THREE_MORE")
            |}
            |
        """.trimMargin()

        compiler(ScalaEmitter()) shouldBeRight scala
    }

    @Test
    fun typeScript() {
        val ts = """
            |export type MyAwesomeEnum = "ONE" | "Two" | "THREE_MORE"
            |
        """.trimMargin()

        compiler(TypeScriptEmitter()) shouldBeRight ts
    }

    @Test
    fun wirespec() {
        val wirespec = """
            |enum MyAwesomeEnum {
            |  ONE, Two, THREE_MORE
            |}
            |
        """.trimMargin()

        compiler(WirespecEmitter()) shouldBeRight wirespec
    }
}
