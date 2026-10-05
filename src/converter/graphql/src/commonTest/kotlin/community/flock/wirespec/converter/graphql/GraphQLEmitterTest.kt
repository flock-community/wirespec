package community.flock.wirespec.converter.graphql

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GraphQLEmitterTest {

    @Test
    fun emitsPlainWirespecAsGraphQL() {
        val warnings = mutableListOf<String>()
        val logger = object : Logger(null) {
            override fun warn(string: String) {
                warnings += string
            }
        }
        val emitted = GraphQLEmitter.emit(
            """
                /* A task */
                type Todo {
                  id: String,
                  title: String,
                  due-date: Integer?,
                  tags: { String }?,
                  owner: User
                }

                type User {
                  name: String
                }

                type Filter {
                  done: Boolean? = false,
                  owner: User?
                }

                enum Status {
                  open, closed
                }

                type Email = String(/.+@.+/g)

                type Item = Todo | User

                rpc FindTodos {
                  filter: Filter,
                  limit: Integer32
                } -> Todo[]

                rpc CreateUser {
                  user: User
                } -> User ! String

                endpoint GetTodo GET /todos/{id: String} -> {
                  200 -> Todo
                }

                channel Events -> Todo
            """.trimIndent().parseWirespec(),
            logger,
        ).single()

        emitted.file shouldBe "schema.graphql"
        emitted.result shouldBe """
            |"A task"
            |type Todo {
            |  id: String!
            |  title: String!
            |  due_date: Long
            |  tags: JSON
            |  owner: User!
            |}
            |
            |type User {
            |  name: String!
            |}
            |
            |input UserInput {
            |  name: String!
            |}
            |
            |input Filter {
            |  done: Boolean = false
            |  owner: UserInput
            |}
            |
            |enum Status {
            |  open
            |  closed
            |}
            |
            |scalar Email
            |
            |union Item = Todo | User
            |
            |type Query {
            |  findTodos(
            |    filter: Filter!
            |    limit: Int!
            |  ): [Todo!]!
            |  createUser(
            |    user: UserInput!
            |  ): User!
            |}
            |
            |scalar Long
            |
            |scalar JSON
            |
        """.trimMargin()
        GraphQLDocumentParser(emitted.result).parseDocument()
        warnings shouldContainExactly listOf(
            "Endpoint GetTodo has no GraphQL counterpart and is left out",
            "Channel Events has no GraphQL counterpart and is left out",
            "The error type of rpc CreateUser has no GraphQL counterpart and is left out",
        )
    }

    private fun String.parseWirespec(): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("todo.ws"), this))).shouldBeRight()
}
