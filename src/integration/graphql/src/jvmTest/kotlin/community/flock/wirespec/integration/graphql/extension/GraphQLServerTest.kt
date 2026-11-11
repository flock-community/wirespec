package community.flock.wirespec.integration.graphql.extension

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.runBlocking
import java.util.Optional
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import community.flock.wirespec.integration.graphql.java.generated.GraphQLClient as JavaGraphQLClient
import community.flock.wirespec.integration.graphql.java.generated.GraphQLServer as JavaGraphQLServer
import community.flock.wirespec.integration.graphql.java.generated.model.ID as JavaID
import community.flock.wirespec.integration.graphql.java.generated.model.NewTodo as JavaNewTodo
import community.flock.wirespec.integration.graphql.java.generated.model.Note as JavaNote
import community.flock.wirespec.integration.graphql.java.generated.model.Notification as JavaNotification
import community.flock.wirespec.integration.graphql.java.generated.model.NotificationsMutation as JavaNotificationsMutation
import community.flock.wirespec.integration.graphql.java.generated.model.NotificationsQuery as JavaNotificationsQuery
import community.flock.wirespec.integration.graphql.java.generated.model.Priority as JavaPriority
import community.flock.wirespec.integration.graphql.java.generated.model.SearchResult as JavaSearchResult
import community.flock.wirespec.integration.graphql.java.generated.model.Tag as JavaTag
import community.flock.wirespec.integration.graphql.java.generated.model.Todo as JavaTodo
import community.flock.wirespec.integration.graphql.java.generated.model.TodoError as JavaTodoError
import community.flock.wirespec.integration.graphql.java.generated.model.User as JavaUser
import community.flock.wirespec.integration.graphql.java.generated.rpc.CompleteTodo as JavaCompleteTodo
import community.flock.wirespec.integration.graphql.kotlin.generated.GraphQLClient as KotlinGraphQLClient
import community.flock.wirespec.integration.graphql.kotlin.generated.GraphQLServer as KotlinGraphQLServer
import community.flock.wirespec.integration.graphql.kotlin.generated.graphql.QueryTodoGraphQL.TodoResult as KotlinTodoResult
import community.flock.wirespec.integration.graphql.kotlin.generated.graphql.QueryTodoGraphQL.UserResult as KotlinUserResult
import community.flock.wirespec.integration.graphql.kotlin.generated.model.ID as KotlinID
import community.flock.wirespec.integration.graphql.kotlin.generated.model.NewTodo as KotlinNewTodo
import community.flock.wirespec.integration.graphql.kotlin.generated.model.Note as KotlinNote
import community.flock.wirespec.integration.graphql.kotlin.generated.model.Notification as KotlinNotification
import community.flock.wirespec.integration.graphql.kotlin.generated.model.NotificationsMutation as KotlinNotificationsMutation
import community.flock.wirespec.integration.graphql.kotlin.generated.model.NotificationsQuery as KotlinNotificationsQuery
import community.flock.wirespec.integration.graphql.kotlin.generated.model.Priority as KotlinPriority
import community.flock.wirespec.integration.graphql.kotlin.generated.model.SearchResult as KotlinSearchResult
import community.flock.wirespec.integration.graphql.kotlin.generated.model.Tag as KotlinTag
import community.flock.wirespec.integration.graphql.kotlin.generated.model.Todo as KotlinTodo
import community.flock.wirespec.integration.graphql.kotlin.generated.model.TodoError as KotlinTodoError
import community.flock.wirespec.integration.graphql.kotlin.generated.model.User as KotlinUser
import community.flock.wirespec.integration.graphql.kotlin.generated.rpc.CompleteTodo as KotlinCompleteTodo
import community.flock.wirespec.integration.jackson.v2.java.WirespecSerialization as JavaSerialization
import community.flock.wirespec.integration.jackson.v2.kotlin.WirespecSerialization as KotlinSerialization
import community.flock.wirespec.java.Wirespec as JavaWirespec
import community.flock.wirespec.kotlin.Wirespec as KotlinWirespec

class GraphQLServerTest {

    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(Jdk8Module())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val kotlinServer = KotlinGraphQLServer(KotlinSerialization(objectMapper), KotlinService())
    private val kotlinClient = KotlinGraphQLClient(KotlinSerialization(objectMapper)) { kotlinServer.handle(it) }

    private val javaServer = JavaGraphQLServer(JavaSerialization(objectMapper), JavaService())
    private val javaClient = JavaGraphQLClient(JavaSerialization(objectMapper)) { javaServer.handle(it) }

    @Test
    fun `Kotlin server answers the generated client with the result of the service`() = runBlocking {
        assertEquals(KotlinTodo(KotlinID("1"), "Groceries", false, KotlinPriority.HIGH, KotlinUser("Ann")), kotlinClient.queryTodo(KotlinID("1")))
        assertNull(kotlinClient.queryTodo(KotlinID("404")))
        assertEquals(listOf("Laundry"), kotlinClient.todos(true).map { it.title })
        assertEquals(listOf("Groceries", "Laundry"), kotlinClient.todos(null).map { it.title })
        assertEquals(KotlinTodo(KotlinID("3"), "Dishes", false, KotlinPriority.LOW, KotlinUser("Ann")), kotlinClient.addTodo(KotlinNewTodo("Dishes", KotlinPriority.LOW)))
        assertEquals(Unit, kotlinClient.clearTodos())
    }

    @Test
    fun `Kotlin server answers with the fields a selection picks`() = runBlocking {
        val todo = kotlinClient.queryTodo(KotlinID("1")) {
            title()
            owner { name() }
        }
        val results = kotlinClient.search("milk") {
            onNote { text() }
            onTag { name() }
        }

        assertEquals(KotlinTodoResult(id = null, title = "Groceries", done = null, priority = null, owner = KotlinUserResult("Ann")), todo)
        assertEquals(listOf("Note" to "milk", "Tag" to "home"), results.map { it.__typename to (it.text ?: it.name) })
    }

    @Test
    fun `Kotlin server returns the error of an rpc with an error type, and the failure of a service`() {
        runBlocking {
            assertEquals(KotlinCompleteTodo.Result(KotlinTodo(KotlinID("1"), "Groceries", true, KotlinPriority.HIGH, KotlinUser("Ann"))), kotlinClient.completeTodo(KotlinID("1")))
            assertEquals(KotlinCompleteTodo.Error(KotlinTodoError("Unknown todo 404")), kotlinClient.completeTodo(KotlinID("404")))
        }
        val error = assertFailsWith<IllegalStateException> { runBlocking { kotlinClient.search("boom") } }
        assertEquals("GraphQL request failed: [GraphQLError(message=Search is down, locations=null, path=[result], extensions=null)]", error.message)
    }

    @Test
    fun `Kotlin server answers the fields of a namespace type`() = runBlocking {
        assertEquals(listOf(KotlinNotification("1", "Hello (nl)"), KotlinNotification("2", "Hello (nl)")), kotlinClient.inbox(2, "nl"))
        assertEquals(3, kotlinClient.inboxCount())
        assertEquals(Unit, kotlinClient.markRead("1"))
    }

    @Test
    fun `Kotlin server reads aliases, arguments, variables with a default and several fields`() {
        val response = kotlin(
            """
            query Overview(${'$'}id: ID!, ${'$'}done: Boolean = true) {
              first: todo(id: ${'$'}id) { id, name: title }
              missing: todo(id: "404") { id }
              todos(done: ${'$'}done) { title priority }
              notifications { inboxCount inbox(limit: 1) { text } }
              __typename
            }
            """,
            variables = mapOf("id" to "1"),
        )

        assertEquals(
            mapOf(
                "data" to mapOf(
                    "first" to mapOf("id" to "1", "name" to "Groceries"),
                    "missing" to null,
                    "todos" to listOf(mapOf("title" to "Laundry", "priority" to null)),
                    "notifications" to mapOf("inboxCount" to 3, "inbox" to listOf(mapOf("text" to "Hello (en)"))),
                    "__typename" to "Query",
                ),
            ),
            response,
        )
    }

    @Test
    fun `Kotlin server reads fragments, type conditions and directives`() {
        val response = kotlin(
            """
            query (${'$'}full: Boolean!) {
              todo(id: "1") { ...Summary owner @include(if: ${'$'}full) { name } done @skip(if: true) }
              search(text: "milk") { __typename ... on Note { text } ... on SearchResult { kind: __typename } ...TagName }
            }
            fragment Summary on Todo { title ... { priority } owner { __typename } }
            fragment TagName on Tag { name }
            """,
            variables = mapOf("full" to true),
        )

        assertEquals(
            mapOf(
                "data" to mapOf(
                    "todo" to mapOf("title" to "Groceries", "priority" to "HIGH", "owner" to mapOf("__typename" to "User", "name" to "Ann")),
                    "search" to listOf(
                        mapOf("__typename" to "Note", "text" to "milk", "kind" to "Note"),
                        mapOf("__typename" to "Tag", "kind" to "Tag", "name" to "home"),
                    ),
                ),
            ),
            response,
        )
    }

    @Test
    fun `Kotlin server reads an input object and runs the fields of a mutation in order`() {
        val response = kotlin(
            """
            mutation {
              added: addTodo(input: { title: "Call \"mom\"", priority: MEDIUM }) { title priority }
              clearTodos
              notifications { markRead(id: "1") }
            }
            """,
        )

        assertEquals(
            mapOf("data" to mapOf("added" to mapOf("title" to "Call \"mom\"", "priority" to "MEDIUM"), "clearTodos" to true, "notifications" to mapOf("markRead" to true))),
            response,
        )
    }

    @Test
    fun `Kotlin server gives an argument that is left out the default of the schema`() {
        val response = kotlin("query (${'$'}size: Int) { notifications { latest { id } one: latest(size: 1) { id } unset: latest(size: ${'$'}size) { id } } }")

        assertEquals(
            mapOf("latest" to listOf(mapOf("id" to "1"), mapOf("id" to "2")), "one" to listOf(mapOf("id" to "1")), "unset" to listOf(mapOf("id" to "1"), mapOf("id" to "2"))),
            response.at("data", "notifications"),
        )
    }

    @Test
    fun `Kotlin server picks the operation that is named`() {
        val document = "query One { todo(id: \"1\") { title } } query Two { notifications { inboxCount } }"

        assertEquals(mapOf("data" to mapOf("notifications" to mapOf("inboxCount" to 3))), kotlin(document, operationName = "Two"))
        assertEquals(mapOf("errors" to listOf(mapOf("message" to "Expected one operation, or an operationName that picks one"))), kotlin(document))
    }

    @Test
    fun `Kotlin server answers a failing field with an error and the other fields with their data`() {
        val response = kotlin("""{ search(text: "boom") { __typename } broken: todo { id } todos { title } }""")

        assertEquals(mapOf("search" to null, "broken" to null, "todos" to listOf(mapOf("title" to "Groceries"), mapOf("title" to "Laundry"))), response["data"])
        assertEquals(listOf(listOf("search"), listOf("broken")), (response["errors"] as List<*>).map { (it as Map<*, *>)["path"] })
        assertEquals("Search is down", ((response["errors"] as List<*>).first() as Map<*, *>)["message"])
    }

    @Test
    fun `Kotlin server rejects a request it cannot execute`() {
        assertEquals(mapOf("errors" to listOf(mapOf("message" to "Cannot query field unknown on type Query"))), kotlin("{ unknown }"))
        assertEquals(mapOf("errors" to listOf(mapOf("message" to "Syntax error in the GraphQL document at 15: Expected a name"))), kotlin("{ todo(id: 1) {"))
        assertEquals(mapOf("errors" to listOf(mapOf("message" to "Cannot execute a subscription"))), kotlin("subscription { todoAdded { id } }"))
        assertEquals(mapOf("errors" to listOf(mapOf("message" to "Fragment Loop spreads itself"))), kotlin("{ ...Loop } fragment Loop on Query { ...Loop }"))
        assertEquals(
            listOf("Cannot query field secret on type Todo"),
            (kotlin("""{ todo(id: "1") { secret } }""")["errors"] as List<*>).map { (it as Map<*, *>)["message"] },
        )
    }

    @Test
    fun `Java server answers the generated client with the result of the service`() {
        assertEquals(Optional.of(JavaTodo(JavaID("1"), "Groceries", false, Optional.of(JavaPriority.HIGH), JavaUser("Ann"))), javaClient.queryTodo(JavaID("1")).join())
        assertEquals(Optional.empty(), javaClient.queryTodo(JavaID("404")).join())
        assertEquals(listOf("Laundry"), javaClient.todos(Optional.of(true)).join().map { it.title() })
        assertEquals("Dishes", javaClient.addTodo(JavaNewTodo("Dishes", Optional.empty())).join().title())
        assertEquals(JavaCompleteTodo.Error(JavaTodoError("Unknown todo 404")), javaClient.completeTodo(JavaID("404")).join())
        assertNull(javaClient.clearTodos().join())
        assertEquals(listOf(JavaNotification("1", "Hello (en)")), javaClient.inbox(1, Optional.empty()).join())
        assertEquals(3, javaClient.inboxCount().join())
        assertEquals(listOf(Optional.of("Groceries"), Optional.of("Laundry")), javaClient.todos(Optional.empty()) { it.title() }.join().map { it.title() })
    }

    @Test
    fun `Java server reads a query of any shape`() {
        val response = java(
            """
            query (${'$'}id: ID!) {
              first: todo(id: ${'$'}id) { ...Summary }
              search(text: "milk") { __typename ... on Note { text } ... on Tag { name } }
              failing: search(text: "boom") { __typename }
              notifications { inboxCount inbox(limit: 1, locale: "nl") { text } }
            }
            fragment Summary on Todo { title priority owner { name } done @skip(if: true) }
            """,
            variables = mapOf("id" to "1"),
        )

        assertEquals(
            mapOf(
                "first" to mapOf("title" to "Groceries", "priority" to "HIGH", "owner" to mapOf("name" to "Ann")),
                "search" to listOf(mapOf("__typename" to "Note", "text" to "milk"), mapOf("__typename" to "Tag", "name" to "home")),
                "failing" to null,
                "notifications" to mapOf("inboxCount" to 3, "inbox" to listOf(mapOf("text" to "Hello (nl)"))),
            ),
            response["data"],
        )
        assertEquals(listOf(mapOf("message" to "Search is down", "path" to listOf("failing"))), response["errors"])
        assertEquals(mapOf("errors" to listOf(mapOf("message" to "Cannot query field unknown on type Mutation"))), java("mutation { unknown }"))
        assertEquals(listOf(2, 1), java("{ notifications { latest { id } one: latest(size: 1) { id } } }").at("data", "notifications").let { listOf(it.at("latest"), it.at("one")) }.map { (it as List<*>).size })
    }

    @Test
    fun `Scala server answers a query and a mutation with the result of the service`() {
        assertEquals(
            "ListMap(data -> ListMap(first -> Some(ListMap(name -> Groceries, owner -> ListMap(name -> Ann))), search -> List(ListMap(__typename -> Note, text -> milk), ListMap(__typename -> Tag)), failing -> None), " +
                "errors -> List(Map(message -> Search is down, path -> List(failing))))",
            scalaCheck("answersAQuery"),
        )
        assertEquals(
            "ListMap(data -> ListMap(clearTodos -> true, completeTodo -> None), errors -> List(TodoError(Unknown todo 404)))",
            scalaCheck("answersAMutation"),
        )
    }

    private fun Any?.at(vararg path: String): Any? = path.fold(this) { value, key -> (value as Map<*, *>)[key] }

    private fun scalaCheck(name: String): Any? = Class.forName("community.flock.wirespec.integration.graphql.extension.ScalaGraphQLServerCheck").getMethod(name).invoke(null)

    private fun kotlin(query: String, variables: Map<String, Any?>? = null, operationName: String? = null): Map<*, *> = runBlocking {
        kotlinServer.handle(KotlinWirespec.RawRequest("POST", listOf("graphql"), emptyMap(), emptyMap(), body(query, variables, operationName)))
            .also { assertEquals(listOf("application/json"), it.headers["Content-Type"]) }
            .let { objectMapper.readValue(it.body, Map::class.java) }
    }

    private fun java(query: String, variables: Map<String, Any?>? = null): Map<*, *> = javaServer
        .handle(JavaWirespec.RawRequest("POST", listOf("graphql"), emptyMap(), emptyMap(), Optional.of(body(query, variables, null))))
        .join()
        .let { objectMapper.readValue(it.body().get(), Map::class.java) }

    private fun body(query: String, variables: Map<String, Any?>?, operationName: String?): ByteArray = objectMapper
        .writeValueAsBytes(listOfNotNull("query" to query.trimIndent(), variables?.let { "variables" to it }, operationName?.let { "operationName" to it }).toMap())

    private class KotlinService : KotlinGraphQLServer.Service {
        private val todos = listOf(
            KotlinTodo(KotlinID("1"), "Groceries", false, KotlinPriority.HIGH, KotlinUser("Ann")),
            KotlinTodo(KotlinID("2"), "Laundry", true, null, KotlinUser("Bob")),
        )

        override suspend fun queryTodo(id: KotlinID): KotlinTodo? = todos.find { it.id == id }

        override suspend fun todos(done: Boolean?): List<KotlinTodo> = todos.filter { done == null || it.done == done }

        override suspend fun search(text: String): List<KotlinSearchResult> = if (text == "boom") error("Search is down") else listOf(KotlinNote(text), KotlinTag("home"))

        override suspend fun addTodo(input: KotlinNewTodo): KotlinTodo = KotlinTodo(KotlinID("3"), input.title, false, input.priority, KotlinUser("Ann"))

        override suspend fun completeTodo(id: KotlinID): KotlinCompleteTodo.Response = todos.find { it.id == id }
            ?.let { KotlinCompleteTodo.Result(it.copy(done = true)) }
            ?: KotlinCompleteTodo.Error(KotlinTodoError("Unknown todo ${id.value}"))

        override suspend fun clearTodos() = Unit

        override suspend fun notifications(): KotlinNotificationsQuery = KotlinNotificationsQuery

        override suspend fun inbox(limit: Int, locale: String?): List<KotlinNotification> = (1..limit).map { KotlinNotification("$it", "Hello (${locale ?: "en"})") }

        override suspend fun inboxCount(): Int = 3

        override suspend fun latest(size: Int): List<KotlinNotification> = inbox(size, "latest")

        override suspend fun mutateNotifications(): KotlinNotificationsMutation = KotlinNotificationsMutation

        override suspend fun markRead(id: String) = Unit
    }

    private class JavaService : JavaGraphQLServer.Service {
        private val todos = listOf(
            JavaTodo(JavaID("1"), "Groceries", false, Optional.of(JavaPriority.HIGH), JavaUser("Ann")),
            JavaTodo(JavaID("2"), "Laundry", true, Optional.empty(), JavaUser("Bob")),
        )

        override fun queryTodo(id: JavaID): CompletableFuture<Optional<JavaTodo>> = done(Optional.ofNullable(todos.find { it.id() == id }))

        override fun todos(done: Optional<Boolean>): CompletableFuture<List<JavaTodo>> = done(todos.filter { done.isEmpty || it.done() == done.get() })

        override fun search(text: String): CompletableFuture<List<JavaSearchResult>> = when (text) {
            "boom" -> CompletableFuture.failedFuture(IllegalStateException("Search is down"))
            else -> CompletableFuture.supplyAsync { listOf(JavaNote(text), JavaTag("home")) }
        }

        override fun addTodo(input: JavaNewTodo): CompletableFuture<JavaTodo> = done(JavaTodo(JavaID("3"), input.title(), false, input.priority(), JavaUser("Ann")))

        override fun completeTodo(id: JavaID): CompletableFuture<JavaCompleteTodo.Response> = done(JavaCompleteTodo.Error(JavaTodoError("Unknown todo ${id.value()}")))

        override fun clearTodos(): CompletableFuture<Void> = CompletableFuture.completedFuture(null)

        override fun notifications(): CompletableFuture<JavaNotificationsQuery> = done(JavaNotificationsQuery())

        override fun inbox(limit: Int, locale: Optional<String>): CompletableFuture<List<JavaNotification>> = done((1..limit).map { JavaNotification("$it", "Hello (${locale.orElse("en")})") })

        override fun inboxCount(): CompletableFuture<Int> = done(3)

        override fun latest(size: Int): CompletableFuture<List<JavaNotification>> = inbox(size, Optional.of("latest"))

        override fun mutateNotifications(): CompletableFuture<JavaNotificationsMutation> = done(JavaNotificationsMutation())

        override fun markRead(id: String): CompletableFuture<Void> = CompletableFuture.completedFuture(null)

        private fun <T> done(value: T): CompletableFuture<T> = CompletableFuture.completedFuture(value)
    }
}
