package community.flock.wirespec.compiler.core.emit

import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.fixture.NodeFixtures
import community.flock.wirespec.compiler.core.parse.Node
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class KotlinEmitterTest {

    private val emitter = object : CompilationContext {
        override val logger = noLogger
        override val emitter = KotlinEmitter(logger = logger)
    }.emitter

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

        val res = emitter.emitFirst(NodeFixtures.type)
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

        val res = emitter.emitFirst(NodeFixtures.refined)
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

        val res = emitter.emitFirst(NodeFixtures.enum)
        res shouldBe expected
    }

    private fun Emitter.emitFirst(node: Node) = emit(listOf(node)).first().result
}
