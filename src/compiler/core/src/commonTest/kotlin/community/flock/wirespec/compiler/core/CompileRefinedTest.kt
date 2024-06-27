package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class CompileRefinedTest {

    private val logger = noLogger

    private val compiler = compile(
        """
        type TodoId /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}/g
        """.trimIndent()
    )

    private val triple = "\"\"\""

    @Test
    fun testRefinedKotlin() {
        val kotlin = """
            package community.flock.wirespec.generated
            
            import community.flock.wirespec.Wirespec
            import kotlin.reflect.typeOf

            data class TodoId(override val value: String): Wirespec.Refined {
              override fun toString() = value
            }
            
            fun TodoId.validate() = Regex(${triple}^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}${triple}).matches(value)
            
        """.trimIndent()

        compiler(KotlinEmitter(logger = logger)) shouldBeRight kotlin
    }

    @Test
    fun testRefinedJava() {
        val java = """
            package community.flock.wirespec.generated;

            import community.flock.wirespec.Wirespec;
            
            public record TodoId (String value) implements Wirespec.Refined {
              @Override
              public String toString() { return value; }
              public static boolean validate(TodoId record) {
                return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}${'$'}").matcher(record.value).find();
              }
              @Override
              public String getValue() { return value; }
            }

        """.trimIndent()

        compiler(JavaEmitter(logger = logger)) shouldBeRight java
    }

    @Test
    fun testRefinedScala() {
        val scala = """
            package community.flock.wirespec.generated

            case class TodoId(val value: String) {
              implicit class TodoIdOps(val that: TodoId) {
                val regex = new scala.util.matching.Regex(${triple}^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}${triple})
                regex.findFirstIn(that.value)
              }
            }
            
            
        """.trimIndent()

        compiler(ScalaEmitter(logger = logger)) shouldBeRight scala
    }

    @Test
    fun testRefinedTypeScript() {
        val ts = """
            |export type TodoId = string;
            |const regExpTodoId = /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}/g;
            |export const validateTodoId = (value: string): value is TodoId => 
            |  regExpTodoId.test(value);
            |""".trimMargin()

        compiler(TypeScriptEmitter(logger = logger)) shouldBeRight ts
    }

    @Test
    fun testRefinedWirespec() {
        val wirespec = """
            type TodoId /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}/g
        
        """.trimIndent()

        compiler(WirespecEmitter(logger = logger)) shouldBeRight wirespec
    }

    private fun compile(source: String) = { emitter: Emitter ->
        WirespecSpec.compile(source)(logger)(emitter)
            .map { it.first().result }
            .onLeft(::println)
    }
}
