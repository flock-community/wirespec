package community.flock.wirespec.converter.graphql

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
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
    fun aTypeImplementsTheInterfaceWhosePartItSpreads() {
        """
            |part Audited {
            |  createdAt: String
            |}
            |
            |part NodeFields {
            |  id: String
            |}
            |
            |@GraphQLInterface("NodeFields")
            |type Node = Todo | Note
            |
            |part Entity {
            |  ...NodeFields,
            |  ...Audited
            |}
            |
            |type Todo {
            |  ...Entity,
            |  title: String
            |}
            |
            |type Note {
            |  ...NodeFields,
            |  text: String
            |}
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
    fun anInterfaceWithoutAPartHasTheFieldsItsMembersShare() {
        """
            |type User {
            |  @Description("The id")
            |  id: String,
            |  @Description("Who to address")
            |  name: String,
            |  active: Boolean,
            |  email: String?
            |}
            |
            |type Bot {
            |  @Description("The id")
            |  id: String,
            |  name: String,
            |  active: Boolean?,
            |  owner: User
            |}
            |
            |@GraphQLInterface
            |type Account = User | Bot
            |
            |rpc Me {} -> Account
        """.trimMargin().toGraphQL() shouldBe """
            |type User implements Account {
            |  "The id"
            |  id: String!
            |  "Who to address"
            |  name: String!
            |  active: Boolean!
            |  email: String
            |}
            |
            |type Bot implements Account {
            |  "The id"
            |  id: String!
            |  name: String!
            |  active: Boolean
            |  owner: User!
            |}
            |
            |interface Account {
            |  "The id"
            |  id: String!
            |  name: String!
            |}
            |
            |type Query {
            |  me: Account!
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

    @Test
    fun writesTheSchemaAndDirectiveDefinitionsOfEveryFileFirst() {
        val schema = """
            |@spec:GraphQLSchema(query: "Root", directives: ["@audit"])
            |
            |@GraphQLQuery("Root")
            |rpc Ping {} -> Boolean
        """.trimMargin()
        val directives = """
            |@spec:GraphQLDirectiveDefinition(name: "audit", locations: ["SCHEMA"])
            |@spec:GraphQLDirectiveDefinition(name: "cached", locations: ["FIELD_DEFINITION"], description: "Kept a while", arguments: { maxAge: { type: "Int", defaultValue: "60" } })
        """.trimMargin()

        GraphQLEmitter.emit(parse(schema, directives), noLogger).single().result shouldBe GraphQLDocumentParser(
            """
            |schema @audit { query: Root }
            |directive @audit on SCHEMA
            |"Kept a while" directive @cached(maxAge: Int = 60) on FIELD_DEFINITION
            |type Root { ping: Boolean! }
            """.trimMargin(),
        ).parseDocument().print()
    }

    @Test
    fun rejectsWhatTheSchemaHasOnceDeclaredInTwoFiles() {
        val todos = """
            |@spec:GraphQLSchema(query: "Root")
            |@spec:GraphQLDirectiveDefinition(name: "cached", locations: ["FIELD_DEFINITION"])
            |@spec:GraphQLSchema(extend: "true", directives: ["@cached"])
            |
            |@GraphQLQuery("Root")
            |rpc Ping {} -> Boolean
        """.trimMargin()
        val notes = """
            |@spec:GraphQLSchema(query: "Query")
            |@spec:GraphQLDirectiveDefinition(name: "cached", locations: ["OBJECT"])
            |@spec:GraphQLSchema(extend: "true", directives: ["@audit"])
        """.trimMargin()

        GraphQLEmitter.validate(parse(todos, notes)).map { it.fileUri.value to it.message } shouldBe listOf(
            "notes.ws" to "The schema definition, a @spec:GraphQLSchema without extend: \"true\", is declared 2 times (in todos.ws, notes.ws), but a GraphQL schema can only have one",
            "notes.ws" to "The directive @cached is declared 2 times (in todos.ws, notes.ws), but a GraphQL schema can only have one",
        )
    }

    @Test
    fun rejectsSchemaAnnotationsOnADefinition() {
        val holders = """
            |@GraphQLSchema(query: "Root")
            |type Schema {}
            |
            |@GraphQLDirectiveDefinition(name: "cached", locations: ["FIELD_DEFINITION"])
            |type CachedDirective {}
        """.trimMargin()

        GraphQLEmitter.validate(parse(holders)).map { it.message } shouldBe listOf(
            "@GraphQLSchema on Schema belongs to the GraphQL schema as a whole, so it belongs in the file header: write it as @spec:GraphQLSchema",
            "@GraphQLDirectiveDefinition on CachedDirective belongs to the GraphQL schema as a whole, so it belongs in the file header: write it as @spec:GraphQLDirectiveDefinition",
        )
    }

    private fun String.toGraphQL(): String = GraphQLEmitter.emit(parse(this), noLogger).single().result

    /** Parses [todos] as `todos.ws` and [notes], if any, as `notes.ws`. */
    private fun parse(todos: String, notes: String? = null): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("todos.ws"), todos)) + listOfNotNull(notes?.let { ModuleContent(FileUri("notes.ws"), it) }))
        .shouldBeRight()
}
