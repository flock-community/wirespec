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

class CompileRefinedTest {

    private val logger = noLogger

    private val compiler = compile(
        """
        refined TodoId /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}/g
        """.trimIndent()
    )

    @Test
    fun testRefinedKotlin() {
        val kotlin = """
            package community.flock.wirespec.generated
            
            import community.flock.wirespec.Wirespec

            data class TodoId(override val value: String): Wirespec.Refined
            fun TodoId.validate() = Regex("^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}").matches(value)
            
        """.trimIndent()

        compiler(KotlinEmitter(logger = logger)) shouldBeRight kotlin
    }

    @Test
    fun testRefinedJava() {
        val java = """
            package community.flock.wirespec.generated;

            import community.flock.wirespec.Wirespec;
            
            public record TodoId (String value) implements Wirespec.Refined {
              static boolean validate(TodoId record) {
                return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}").matcher(record.value).find();
              }
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
                val regex = new scala.util.matching.Regex("^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}")
                regex.findFirstIn(that.value)
              }
            }
            
            
        """.trimIndent()

        compiler(ScalaEmitter(logger = logger)) shouldBeRight scala
    }

    @Test
    fun testRefinedTypeScript() {
        val ts = """
            export type TodoId = {
              value: string
            }
            const validateTodoId = (type: TodoId) => (new RegExp('^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}')).test(type.value);


        """.trimIndent()

        compiler(TypeScriptEmitter(logger = logger)) shouldBeRight ts
    }

    @Test
    fun testEnumWirespec() {
        val wirespec = """
            refined TodoId /^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}/g
        
        """.trimIndent()

        compiler(WirespecEmitter(logger = logger)) shouldBeRight wirespec
    }

    private fun compile(source: String) = { emitter: AbstractEmitter ->
        Wirespec.compile(source)(logger)(emitter)
            .map { it.first().result }
            .onLeft(::println)
    }
}
