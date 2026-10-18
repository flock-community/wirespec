package community.flock.wirespec.converter.graphql

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.converter.graphql.GraphQLDocumentPrinter.print
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GraphQLEmitterTest {

    @Test
    fun emitsTheSchemaThatWasConvertedToWirespec() {
        val graphql = """
            |"A task to do"
            |type Todo {
            |  id: ID!
            |  done: Boolean!
            |}
            |
            |type Query {
            |  todo(id: ID!): Todo
            |}
        """.trimMargin()

        GraphQLParser.parse(ModuleContent(FileUri("schema.graphql"), graphql), strict = true)
            .let { GraphQLEmitter.emit(it, noLogger).single() } shouldBe Emitted("schema.graphql", GraphQLDocumentParser(graphql).parseDocument().print())
    }

    @Test
    fun emitsWirespecThatDoesNotComeFromGraphQL() {
        """
            |type TodoIdentifier = String(/^[0-9a-f]+$/g)
            |
            |part Audited {
            |  createdAt: String
            |}
            |
            |enum Prio {
            |  LOW, MEDIUM, HIGH
            |}
            |
            |// A task to do
            |type Todo {
            |  id: TodoIdentifier,
            |  prio: Prio = MEDIUM,
            |  tags: String[]?,
            |  ...Audited
            |}
            |
            |type TodoError {
            |  reason: String
            |}
            |
            |endpoint GetTodos GET /todos -> {
            |  200 -> Todo[]
            |}
            |
            |channel TodoUpdates -> Todo
            |
            |rpc SearchTodos {
            |  query: String,
            |  limit: Integer?
            |} -> Todo[]
            |
            |rpc CompleteTodo {
            |  id: TodoIdentifier
            |} -> Todo ! TodoError
        """.trimMargin().toGraphQL() shouldBe """
            |scalar TodoIdentifier
            |
            |enum Prio {
            |  LOW
            |  MEDIUM
            |  HIGH
            |}
            |
            |"A task to do"
            |type Todo {
            |  id: TodoIdentifier!
            |  prio: Prio!
            |  tags: [String!]
            |  createdAt: String!
            |}
            |
            |type TodoError {
            |  reason: String!
            |}
            |
            |type Query {
            |  searchTodos(
            |    query: String!
            |    limit: Int
            |  ): [Todo!]!
            |  completeTodo(
            |    id: TodoIdentifier!
            |  ): Todo!
            |}
            |
        """.trimMargin()
    }

    @Test
    fun aTypeImplementsTheInterfacePartsItSpreads() {
        """
            |part Audited {
            |  createdAt: String
            |}
            |
            |@GraphQLInterface
            |part NodeInterface {
            |  id: String
            |}
            |
            |part Entity {
            |  ...NodeInterface,
            |  ...Audited
            |}
            |
            |type Todo {
            |  ...Entity,
            |  title: String
            |}
            |
            |type Note {
            |  ...NodeInterface,
            |  text: String
            |}
            |
            |type Node = Todo | Note
            |
            |rpc GetNode {
            |  id: String
            |} -> Node
        """.trimMargin().toGraphQL() shouldBe """
            |interface Node {
            |  id: String!
            |}
            |
            |type Todo implements Node {
            |  id: String!
            |  createdAt: String!
            |  title: String!
            |}
            |
            |type Note implements Node {
            |  id: String!
            |  text: String!
            |}
            |
            |type Query {
            |  getNode(
            |    id: String!
            |  ): Node!
            |}
            |
        """.trimMargin()
    }

    @Test
    fun writesTheTypesAnRpcTakesAsInputObjects() {
        """
            |type Person {
            |  name: String
            |}
            |
            |type Todo {
            |  title: String,
            |  owner: Person
            |}
            |
            |type Filter {
            |  owner: Person?,
            |  done: Boolean? = false
            |}
            |
            |type TodoInput {
            |  title: String
            |}
            |
            |rpc AddTodo {
            |  todo: Todo
            |} -> Todo
            |
            |rpc FindTodos {
            |  filter: Filter
            |} -> Todo[]
        """.trimMargin().toGraphQL() shouldBe """
            |type Person {
            |  name: String!
            |}
            |
            |input PersonInput {
            |  name: String!
            |}
            |
            |type Todo {
            |  title: String!
            |  owner: Person!
            |}
            |
            |input TodoInput2 {
            |  title: String!
            |  owner: PersonInput!
            |}
            |
            |input Filter {
            |  owner: PersonInput
            |  done: Boolean = false
            |}
            |
            |type TodoInput {
            |  title: String!
            |}
            |
            |type Query {
            |  addTodo(
            |    todo: TodoInput2!
            |  ): Todo!
            |  findTodos(
            |    filter: Filter!
            |  ): [Todo!]!
            |}
            |
        """.trimMargin()
    }

    @Test
    fun mapsWhatGraphQLHasNoTypeFor() {
        """
            |type Attachment {
            |  content: Bytes,
            |  metadata: Any?,
            |  labels: { String }
            |}
            |
            |rpc Ping {} -> Unit
        """.trimMargin().toGraphQL() shouldBe """
            |type Attachment {
            |  content: String!
            |  metadata: JSON
            |  labels: JSON!
            |}
            |
            |type Query {
            |  ping: Boolean!
            |}
            |
            |scalar JSON
            |
        """.trimMargin()
    }

    private fun String.toGraphQL(): String = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("todo.ws"), this)))
        .shouldBeRight()
        .let { GraphQLEmitter.emit(it, noLogger).single().result }
}
