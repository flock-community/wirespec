package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldContain
import kotlin.test.Test

class CompilerTest {

    private val logger = noLogger

    @Test
    fun testCompileKotlin() {

        val source = """
            type Bla {
              foo: String,
              bar: String
            }
            
        """.trimIndent()

        val out = """
            package community.flock.wirespec.generated

            data class Bla(
              val foo: String,
              val bar: String
            )
            
        """.trimIndent()

        Wirespec.compile(source)(logger)(KotlinEmitter(logger = logger))
            .map { it.first().result } shouldBeRight out
    }

    @Test
    fun testCompileKotlinMissingColumnInBody() {

        val source = """
            type Bla {
              foo: String
              bar: String
            }
            
        """.trimIndent()

        Wirespec.compile(source)(logger)(KotlinEmitter(logger = logger))
            .shouldBeLeft()
            .map { it.message } shouldContain "RightCurly expected, not: CustomValue at line 3 and position 3"
    }

    @Test
    fun testRefinedType() {
        val source = """
            refined Name /^[a-zA-Z]{1,50}$/g
            
        """.trimIndent()

        val out = """
            package community.flock.wirespec.generated
            
            import community.flock.wirespec.Wirespec
            
            data class Name(override val value: String): Wirespec.Refined
            fun Name.validate() = Regex(${"\"\"\""}^[a-zA-Z]{1,50}$${"\"\"\""}).matches(value)
            
        """.trimIndent()

        Wirespec.compile(source)(logger)(KotlinEmitter(logger = logger))
            .map { it.first().result }
            .onLeft(::println) shouldBeRight out
    }
}
