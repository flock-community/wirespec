package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class CompileRefinedTest {

    private val compiler = """
        |type TodoId /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}/g
    """.trimMargin().let(::compile)

    @Test
    fun kotlin() {
        val kotlin = """
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TodoId(override val value: String): Wirespec.Refined {
            |  override fun toString() = value
            |}
            |
            |fun TodoId.validate() = Regex(""${'"'}^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}""${'"'}).matches(value)
            |
        """.trimMargin()

        compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun java() {
        val java = """
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TodoId (String value) implements Wirespec.Refined {
            |  @Override
            |  public String toString() { return value; }
            |  public static boolean validate(TodoId record) {
            |    return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}${'$'}").matcher(record.value).find();
            |  }
            |  @Override
            |  public String getValue() { return value; }
            |}
            |
        """.trimMargin()

        compiler { JavaEmitter() } shouldBeRight java
    }

    @Test
    fun typeScript() {
        val ts = """
            |export type TodoId = string;
            |const regExpTodoId = /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}/g;
            |export const validateTodoId = (value: string): value is TodoId => 
            |  regExpTodoId.test(value);
            |
        """.trimMargin()

        compiler { TypeScriptEmitter() } shouldBeRight ts
    }

    @Test
    fun wirespec() {
        val wirespec = """
            |type TodoId /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}/g
            |
        """.trimMargin()

        compiler { WirespecEmitter() } shouldBeRight wirespec
    }
}