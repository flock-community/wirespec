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
            refined Name "^.{1,10}$"
            
            type TodoId {
              value: String
            }
            
            type Todo {
              id: TodoId,
              name: Name,
              done: Boolean
            }
            
            type TodoInput {
              name: String,
              done: Boolean
            }
            
            endpoint FindAll GET /todos?{done: Boolean} Todos[]
            endpoint FindById GET /todos/{id: String} Todos?
            endpoint Create POST /todos TodoInput -> Todos
            endpoint Update PUT /todos TodoInput -> Todos
            
        """.trimIndent()

        val res = WireSpec.tokenize(source)
            .let { Parser(logger).parse(it).fold(
                ifRight = { ex -> ex },
                ifLeft = { err -> throw err }
            ) }

        println("=========")
        println(res)
        println("=========")

    }


}