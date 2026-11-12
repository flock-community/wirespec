package community.flock.wirespec.converter.graphql

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
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
    fun writesAnRpcAsAFieldNextToOthers() {
        """
            |type Todo {
            |  id: ID!
            |}
            |
            |type Query {
            |  todos(first: Int = 10, done: Boolean): [Todo!]!
            |  count: Int!
            |}
        """.trimMargin().fromGraphQL().let { definitions ->
            val operations = GraphQLOperations(definitions)
            val rpcs = definitions.filterIsInstance<Rpc>().associateBy { it.identifier.value }

            operations.field(rpcs.getValue("Todos"), "todos") { "todos_$it" } shouldBe "todos: todos(first: \$todos_first, done: \$todos_done)"
            operations.variables(rpcs.getValue("Todos")) { "todos_$it" } shouldBe listOf("\$todos_first: Int = 10", "\$todos_done: Boolean")
            operations.field(rpcs.getValue("Count"), "count") shouldBe "count: count"
            operations.variables(rpcs.getValue("Count")) shouldBe emptyList()
            operations.parent(rpcs.getValue("Count")) shouldBe "Query"
        }
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
        val definitions = """
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
        val operations = GraphQLOperations(definitions)
        val rpcs = definitions.filterIsInstance<Rpc>().associateBy { it.identifier.value }
        val todo = definitions.filterIsInstance<community.flock.wirespec.compiler.core.parse.ast.Type>().single { it.identifier.value == "Todo" }

        operations.open(rpcs.getValue("QueryTodo"), "result") shouldBe "query QueryTodo(\$id: ID!) { result: todo(id: \$id)"
        operations.close(rpcs.getValue("QueryTodo")) shouldBe " }"
        operations.document(rpcs.getValue("QueryTodo"), "result") shouldBe "query QueryTodo(\$id: ID!) { result: todo(id: \$id) { id } }"
        operations.composite(rpcs.getValue("QueryTodo").result) shouldBe todo
        operations.composite(rpcs.getValue("Count").result) shouldBe null
        todo.shape.value.filterIsInstance<community.flock.wirespec.compiler.core.parse.ast.Field>().map { operations.isSelectable(it) } shouldBe listOf(true, false)
    }

    @Test
    fun nestsAFieldOfANamespaceTypeInTheFieldsThatLeadToIt() {
        val definitions = """
            |type Notification {
            |  id: String
            |}
            |
            |type NotificationsQuery {}
            |
            |type InboxQuery {}
            |
            |type NotificationsMutation {}
            |
            |@GraphQLQuery
            |rpc Notifications {} -> NotificationsQuery
            |
            |@GraphQLQuery("NotificationsQuery")
            |@GraphQLName("inbox")
            |rpc InboxNamespace {} -> InboxQuery
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
            |@GraphQLQuery("InboxQuery")
            |rpc Unread {} -> Notification[]
            |
            |@GraphQLMutation
            |@GraphQLName("notifications")
            |rpc MutateNotifications {} -> NotificationsMutation
            |
            |@GraphQLMutation("NotificationsMutation")
            |rpc MarkRead {
            |  id: String
            |} -> Unit
        """.trimMargin().parsed()
        val operations = GraphQLOperations(definitions)
        val rpcs = definitions.filterIsInstance<Rpc>().associateBy { it.identifier.value }

        definitions.documents() shouldBe mapOf(
            "Notifications" to "query Notifications { result: notifications { __typename } }",
            "InboxNamespace" to "query InboxNamespace { notifications { result: inbox { __typename } } }",
            "Inbox" to "query Inbox(\$limit: Int!, \$locale: String) { notifications { result: inbox(limit: \$limit, locale: \$locale) { id } } }",
            "InboxCount" to "query InboxCount { notifications { result: inboxCount } }",
            "Unread" to "query Unread { notifications { inbox { result: unread { id } } } }",
            "MutateNotifications" to "mutation MutateNotifications { result: notifications { __typename } }",
            "MarkRead" to "mutation MarkRead(\$id: String!) { notifications { result: markRead(id: \$id) } }",
        )
        operations.path(rpcs.getValue("Notifications")) shouldBe emptyList()
        operations.path(rpcs.getValue("Unread")) shouldBe listOf("notifications", "inbox")
        operations.open(rpcs.getValue("Unread"), "result") shouldBe "query Unread { notifications { inbox { result: unread"
        operations.close(rpcs.getValue("Unread")) shouldBe " } } }"
        rpcs.mapValues { (_, rpc) -> operations.parent(rpc) }.filterKeys { it in setOf("Notifications", "Inbox", "Unread", "MarkRead") } shouldBe mapOf(
            "Notifications" to "Query",
            "Inbox" to "NotificationsQuery",
            "Unread" to "InboxQuery",
            "MarkRead" to "NotificationsMutation",
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
    fun failsOnAFieldOfATypeThatNoFieldLeadsTo() {
        shouldThrow<IllegalStateException> {
            """
                |@GraphQLQuery
                |rpc Notifications {} -> Unit
                |
                |@GraphQLQuery("NotificationsQuery")
                |rpc InboxCount {} -> Integer32
            """.trimMargin().parsed().documents()
        }.message shouldContain "rpc InboxCount: it is a field of NotificationsQuery, but no field leads from Query to that type"
    }

    private fun String.fromGraphQL(): List<Definition> = GraphQLParser.parse(ModuleContent(FileUri("schema.graphql"), this), strict = true)
        .let { WirespecEmitter().emit(it, noLogger).single().result }
        .parsed()

    private fun String.parsed(): List<Definition> = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("schema.ws"), this)))
        .shouldBeRight()
        .modules.flatMap { it.statements }

    private fun List<Definition>.documents(): Map<String, String> = GraphQLOperations(this).let { operations ->
        filterIsInstance<Rpc>().mapNotNull { rpc -> operations.document(rpc, "result")?.let { rpc.identifier.value to it } }.toMap()
    }
}
