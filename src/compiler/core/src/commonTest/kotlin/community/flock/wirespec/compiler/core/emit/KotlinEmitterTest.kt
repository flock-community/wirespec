package community.flock.wirespec.compiler.core.emit

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.fixture.NodeFixtures
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class KotlinEmitterTest {

    private val emitContext = object : EmitContext, NoLogger {
        override val emitter = KotlinEmitter()
    }

    @Test
    fun testEmitterType() {
        val expected = """
            |package community.flock.wirespec.generated
            |
            |data class Todo(
            |  val name: String,
            |  val description: String?,
            |  val notes: List<String>,
            |  val done: Boolean
            |)
            |
        """.trimMargin()

        val res = emitContext.emitFirst(NodeFixtures.type)
        res shouldBe expected
    }

    @Test
    fun testEmitterRefined() {
        val expected = """
            |package community.flock.wirespec.generated
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class UUID(override val value: String): Wirespec.Refined {
            |  override fun toString() = value
            |}
            |
            |fun UUID.validate() = Regex(${"\"\"\""}^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}${"\"\"\""}).matches(value)
            |
        """.trimMargin()

        val res = emitContext.emitFirst(NodeFixtures.refined)
        res shouldBe expected
    }

    @Test
    fun testEmitterEnum() {
        val expected = """
            |package community.flock.wirespec.generated
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |enum class TodoStatus (override val label: String): Wirespec.Enum {
            |  OPEN("OPEN"),
            |  IN_PROGRESS("IN_PROGRESS"),
            |  CLOSE("CLOSE");
            |  override fun toString(): String {
            |    return label
            |  }
            |}
            |
        """.trimMargin()

        val res = emitContext.emitFirst(NodeFixtures.enum)
        res shouldBe expected
    }

    private fun EmitContext.emitFirst(node: Definition) = emitter.emit(Module("", nonEmptyListOf(node)), logger).first().result
}
