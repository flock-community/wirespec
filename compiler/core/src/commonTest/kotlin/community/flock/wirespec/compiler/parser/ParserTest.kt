package community.flock.wirespec.compiler.parser

import community.flock.wirespec.compiler.core.WireSpec
import community.flock.wirespec.compiler.core.parse.Parser
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.Logger
import kotlin.test.Test

class ParserTest {

    private val logger: Logger = object : Logger(enableLogging = true) {}

    @Test
    fun testCompileKotlin() {
        val source = """
            type Todo {
              id: String,
              name: String,
              done: Boolean
            }
            
            type TodoInput {
              name: String,
              done: Boolean
            }
            
            endpoint GET /todos?{done: Boolean} Todos
            endpoint GET /todos/{id: String} Todos
            endpoint POST /todos TodoInput -> Todos
            
        """.trimIndent()

        val res = WireSpec.tokenize(source)
            .let { Parser(logger).parse(it) }

        println("=========")
        println(res)
        println("=========")

    }


}