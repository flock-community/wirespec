package community.flock.wirespec.converter.graphql

import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class GraphQLOperationsTest {

    @Test
    fun selectsEveryFieldOfTheResult() {
        """
            |type Todo {
            |  id: ID!
            |  title: String!
            |  owner: User!
            |  tags: [String!]
            |}
            |
            |type User {
            |  name: String!
            |}
            |
            |type Query {
            |  todo(id: ID!): Todo
            |  todos: [Todo!]!
            |  count: Int!
            |}
        """.trimMargin().fromGraphQL().documents() shouldBe mapOf(
            "QueryTodo" to "query QueryTodo(\$id: ID!) { result: todo(id: \$id) { id title owner { name } tags } }",
            "Todos" to "query Todos { result: todos { id title owner { name } tags } }",
            "Count" to "query Count { result: count }",
        )
    }

    @Test
    fun passesArgumentsAsVariablesWithTheirDefaults() {
        """
            |enum Order { ASC DESC }
            |
            |input Filter {
            |  done: Boolean
            |}
            |
            |type Todo {
            |  id: ID!
            |}
            |
            |type Query {
            |  search(filter: Filter!, first: Int = 10, order: Order = ASC): [Todo!]!
            |}
            |
            |type Mutation {
            |  complete(id: ID!): Todo!
            |}
            |
            |type Subscription {
            |  completed: Todo!
            |}
        """.trimMargin().fromGraphQL().documents() shouldBe mapOf(
            "Search" to "query Search(\$filter: Filter!, \$first: Int = 10, \$order: Order = ASC) " +
                "{ result: search(filter: \$filter, first: \$first, order: \$order) { id } }",
            "Complete" to "mutation Complete(\$id: ID!) { result: complete(id: \$id) { id } }",
        )
    }

    @Test
    fun selectsTheMembersOfUnionsAndInterfaces() {
        """
            |interface Node {
            |  id: ID!
            |}
            |
            |type User implements Node {
            |  id: ID!
            |  email: String
            |}
            |
            |type Post implements Node {
            |  id: ID!
            |  title: String!
            |}
            |
            |union SearchResult = User | Post
            |
            |type Query {
            |  node(id: ID!): Node
            |  search(text: String!): [SearchResult!]!
            |}
        """.trimMargin().fromGraphQL().documents() shouldBe mapOf(
            "QueryNode" to "query QueryNode(\$id: ID!) { result: node(id: \$id) { __typename ... on User { id email } ... on Post { id title } } }",
            "Search" to "query Search(\$text: String!) " +
                "{ result: search(text: \$text) { __typename ... on User { id email } ... on Post { id title } } }",
        )
    }

    @Test
    fun usesTheGraphQLNamesOfRenamedTypes() {
        """
            |type _Service {
            |  sdl: String
            |}
            |
            |union Entity = _Service
            |
            |type Query {
            |  entities: [Entity]!
            |}
        """.trimMargin().fromGraphQL().documents() shouldBe mapOf(
            "Entities" to "query Entities { result: entities { __typename ... on _Service { sdl } } }",
        )
    }

    @Test
    fun leavesOutNullableFieldsThatCannotBeSelected() {
        """
            |type Todo {
            |  id: ID!
            |  parent: Todo
            |  owner: User!
            |  comments(first: Int!): [String!]
            |}
            |
            |type User {
            |  name: String!
            |  todos: [Todo!]
            |}
            |
            |type Query {
            |  todo(id: ID!): Todo
            |}
        """.trimMargin().fromGraphQL().documents() shouldBe mapOf(
            "QueryTodo" to "query QueryTodo(\$id: ID!) { result: todo(id: \$id) { id owner { name } } }",
        )
    }

    @Test
    fun failsOnARequiredFieldThatRefersBack() {
        shouldThrow<IllegalStateException> {
            """
                |type Todo {
                |  id: ID!
                |  owner: User!
                |}
                |
                |type User {
                |  todos: [Todo!]!
                |}
                |
                |type Query {
                |  todo(id: ID!): Todo
                |}
            """.trimMargin().fromGraphQL().documents()
        }.message shouldContain "rpc QueryTodo: field owner.todos of its result refers back to a type it is selected in"
    }

    @Test
    fun treatsAnRpcThatDoesNotComeFromGraphQLAsAQuery() {
        """
            |type Todo {
            |  id: String,
            |  done: Boolean
            |}
            |
            |rpc SearchTodos {
            |  query: String,
            |  limit: Integer?
            |} -> Todo[]
        """.trimMargin().parsed().documents() shouldBe mapOf(
            "SearchTodos" to "query SearchTodos(\$query: String!, \$limit: Int) { result: searchTodos(query: \$query, limit: \$limit) { id done } }",
        )
    }

    @Test
    fun opensTheDocumentWhereTheSelectionGoes() {
        val ast = """
            |type Todo {
            |  id: ID!
            |  comments(first: Int!): [String!]
            |}
            |
            |type Query {
            |  todo(id: ID!): Todo
            |  count: Int!
            |}
        """.trimMargin().fromGraphQL()
        val operations = GraphQLOperations(ast)
        val rpcs = ast.definitions().filterIsInstance<Rpc>().associateBy { it.identifier.value }
        val todo = ast.definitions().filterIsInstance<community.flock.wirespec.compiler.core.parse.ast.Type>().single { it.identifier.value == "Todo" }

        operations.open(rpcs.getValue("QueryTodo"), "result") shouldBe "query QueryTodo(\$id: ID!) { result: todo(id: \$id)"
        operations.close(rpcs.getValue("QueryTodo")) shouldBe " }"
        operations.document(rpcs.getValue("QueryTodo"), "result") shouldBe "query QueryTodo(\$id: ID!) { result: todo(id: \$id) { id } }"
        operations.composite(rpcs.getValue("QueryTodo").result) shouldBe todo
        operations.composite(rpcs.getValue("Count").result) shouldBe null
        operations.document(rpcs.getValue("TodoComments"), "result") shouldBe null
    }

    @Test
    fun nestsAnOperationInTheFieldsOfThePathToTheTypeItNames() {
        val ast = """
            |@file:GraphQLQuery(path: { notifications: "NotificationsQuery!" })
            |@file:GraphQLQuery(path: { notifications: "NotificationsQuery!", archive: { type: "ArchiveQuery", description: "What was read" } })
            |@file:GraphQLMutation(path: { notifications: "NotificationsMutation!" })
            |
            |type Notification {
            |  id: String
            |}
            |
            |@GraphQLQuery
            |rpc Ping {} -> Boolean
            |
            |@GraphQLQuery("NotificationsQuery")
            |rpc Inbox {
            |  limit: Integer32,
            |  locale: String?
            |} -> Notification[]
            |
            |@GraphQLQuery("NotificationsQuery")
            |rpc InboxCount {} -> Integer32
            |
            |@GraphQLQuery("ArchiveQuery")
            |rpc Unread {} -> Notification[]
            |
            |@GraphQLMutation("NotificationsMutation")
            |rpc MarkRead {
            |  id: String
            |} -> Unit
        """.trimMargin().parsed()
        val operations = GraphQLOperations(ast)
        val rpcs = ast.definitions().filterIsInstance<Rpc>().associateBy { it.identifier.value }

        ast.documents() shouldBe mapOf(
            "Ping" to "query Ping { result: ping }",
            "Inbox" to "query Inbox(\$limit: Int!, \$locale: String) { notifications { result: inbox(limit: \$limit, locale: \$locale) { id } } }",
            "InboxCount" to "query InboxCount { notifications { result: inboxCount } }",
            "Unread" to "query Unread { notifications { archive { result: unread { id } } } }",
            "MarkRead" to "mutation MarkRead(\$id: String!) { notifications { result: markRead(id: \$id) } }",
        )
        operations.path(rpcs.getValue("Ping")) shouldBe emptyList()
        operations.path(rpcs.getValue("InboxCount")) shouldBe listOf("notifications")
        operations.path(rpcs.getValue("Unread")) shouldBe listOf("notifications", "archive")
        operations.open(rpcs.getValue("Unread"), "result") shouldBe "query Unread { notifications { archive { result: unread"
        operations.close(rpcs.getValue("Unread")) shouldBe " } } }"
    }

    @Test
    fun nestsTheOperationsOfATypeNestedInARootType() {
        """
            |type Query {
            |  notifications: NotificationsQuery!
            |}
            |
            |type NotificationsQuery {
            |  inbox(limit: Int!): [Notification!]!
            |  inboxCount: Int!
            |}
            |
            |type Notification {
            |  id: ID!
            |}
        """.trimMargin().fromGraphQL().documents() shouldBe mapOf(
            "Inbox" to "query Inbox(\$limit: Int!) { notifications { result: inbox(limit: \$limit) { id } } }",
            "InboxCount" to "query InboxCount { notifications { result: inboxCount } }",
        )
    }

    @Test
    fun keepsAFieldOfARenamedRootTypeAtTheTopLevel() {
        """
            |schema { query: Root }
            |
            |type Root {
            |  ping: Boolean
            |}
        """.trimMargin().fromGraphQL().documents() shouldBe mapOf("Ping" to "query Ping { result: ping }")
    }

    @Test
    fun takesTheRootTypesFromTheFileHeader() {
        """
            |@spec:GraphQLSchema(query: "Root", mutation: "Commands")
            |
            |@GraphQLQuery("Root")
            |rpc Ping {} -> Boolean
            |
            |@GraphQLMutation("Commands")
            |rpc Reset {} -> Unit
        """.trimMargin().parsed().documents() shouldBe mapOf(
            "Ping" to "query Ping { result: ping }",
            "Reset" to "mutation Reset { result: reset }",
        )
    }

    @Test
    fun failsOnAnOperationOfATypeThatIsNotTheRootType() {
        shouldThrow<IllegalStateException> {
            """
                |@GraphQLQuery("NotificationsQuery")
                |rpc InboxCount {} -> Integer32
            """.trimMargin().parsed().documents()
        }.message shouldContain "rpc InboxCount: it is a field of NotificationsQuery, which is not the root type Query. " +
            "Name the fields that lead from Query to its type in the header of its file, like `@file:GraphQLQuery(path: { field: \"NotificationsQuery\" })`."
    }

    private fun String.fromGraphQL(): AST = GraphQLParser.parse(ModuleContent(FileUri("schema.graphql"), this), strict = true)
        .let { WirespecEmitter().emit(it, noLogger) }
        .let { files -> parsed(*files.map { it.result }.toTypedArray()) }

    private fun String.parsed(): AST = parsed(this)

    private fun parsed(vararg files: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(files.mapIndexed { index, file -> ModuleContent(FileUri("schema$index.ws"), file) }.toNonEmptyListOrNull()!!)
        .shouldBeRight()

    private fun AST.definitions(): List<Definition> = modules.flatMap { it.statements }

    private fun AST.documents(): Map<String, String> = GraphQLOperations(this).let { operations ->
        definitions().filterIsInstance<Rpc>().mapNotNull { rpc -> operations.document(rpc, "result")?.let { rpc.identifier.value to it } }.toMap()
    }
}
