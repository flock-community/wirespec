package community.flock.wirespec.compiler.core.emit

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.fixture.NodeFixtures
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class JavaEmitterTest {

    private val emitContext = object : EmitContext, NoLogger {
        override val emitters = nonEmptySetOf(JavaEmitter())
    }

    @Test
    fun testEmitterType() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model;
            |
            |public record Todo (
            |  String name,
            |  java.util.Optional<String> description,
            |  java.util.List<String> notes,
            |  Boolean done
            |) {
            |};
            |
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.type)
        res shouldBe expected
    }

    @Test
    fun testEmitterEmptyType() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model;
            |
            |public record TodoWithoutProperties (
            |
            |) {
            |};
            |
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.emptyType)
        res shouldBe expected
    }

    @Test
    fun testEmitterRefined() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record UUID (String value) implements Wirespec.Refined {
            |  @Override
            |  public String toString() { return value; }
            |  public static boolean validate(UUID record) {
            |    return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}${'$'}").matcher(record.value).find();
            |  }
            |  @Override
            |  public String getValue() { return value; }
            |}
            |
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.refined)
        res shouldBe expected
    }

    @Test
    fun testEmitterEnum() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public enum TodoStatus implements Wirespec.Enum {
            |  OPEN("OPEN"),
            |  IN_PROGRESS("IN_PROGRESS"),
            |  CLOSE("CLOSE");
            |  public final String label;
            |  TodoStatus(String label) {
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
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.enum)
        res shouldBe expected
    }

    private fun EmitContext.emitFirst(node: Definition) = emitters.map { it.emit(Module("", nonEmptyListOf(node)), logger).first().result }
}
