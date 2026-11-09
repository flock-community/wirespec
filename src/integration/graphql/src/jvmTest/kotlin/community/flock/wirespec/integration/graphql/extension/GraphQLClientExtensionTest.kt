package community.flock.wirespec.integration.graphql.extension

import arrow.core.nonEmptyListOf
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import kotlinx.coroutines.runBlocking
import java.util.Optional
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import community.flock.wirespec.integration.graphql.java.generated.graphql.GraphQLClient as JavaGraphQLClient
import community.flock.wirespec.integration.graphql.java.generated.graphql.TodosGraphQL.TodoResult as JavaTodoResult
import community.flock.wirespec.integration.graphql.java.generated.graphql.TodosGraphQL.UserResult as JavaUserResult
import community.flock.wirespec.integration.graphql.java.generated.model.ID as JavaID
import community.flock.wirespec.integration.graphql.java.generated.model.Priority as JavaPriority
import community.flock.wirespec.integration.graphql.java.generated.model.Todo as JavaTodo
import community.flock.wirespec.integration.graphql.java.generated.model.TodoError as JavaTodoError
import community.flock.wirespec.integration.graphql.java.generated.model.User as JavaUser
import community.flock.wirespec.integration.graphql.java.generated.rpc.CompleteTodo as JavaCompleteTodo
import community.flock.wirespec.integration.graphql.kotlin.generated.graphql.GraphQLClient as KotlinGraphQLClient
import community.flock.wirespec.integration.graphql.kotlin.generated.graphql.QueryTodoGraphQL.TodoResult as KotlinTodoResult
import community.flock.wirespec.integration.graphql.kotlin.generated.graphql.QueryTodoGraphQL.UserResult as KotlinUserResult
import community.flock.wirespec.integration.graphql.kotlin.generated.model.ID as KotlinID
import community.flock.wirespec.integration.graphql.kotlin.generated.model.NewTodo as KotlinNewTodo
import community.flock.wirespec.integration.graphql.kotlin.generated.model.Priority as KotlinPriority
import community.flock.wirespec.integration.graphql.kotlin.generated.model.Todo as KotlinTodo
import community.flock.wirespec.integration.graphql.kotlin.generated.model.TodoError as KotlinTodoError
import community.flock.wirespec.integration.graphql.kotlin.generated.model.User as KotlinUser
import community.flock.wirespec.integration.graphql.kotlin.generated.rpc.CompleteTodo as KotlinCompleteTodo
import community.flock.wirespec.integration.jackson.v2.java.WirespecSerialization as JavaSerialization
import community.flock.wirespec.integration.jackson.v2.kotlin.WirespecSerialization as KotlinSerialization
import community.flock.wirespec.java.Wirespec as JavaWirespec
import community.flock.wirespec.kotlin.Wirespec as KotlinWirespec

class GraphQLClientExtensionTest {

    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(Jdk8Module())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @Test
    fun `Kotlin client posts the query with its variables and reads the result`() = runBlocking {
        val server = KotlinServer("""{"data":{"result":{"id":"1","title":"Groceries","done":false,"priority":"HIGH","owner":{"name":"Ann"}}}}""")

        val todo = server.client().queryTodo(KotlinID("1"))

        assertEquals(KotlinTodo(KotlinID("1"), "Groceries", false, KotlinPriority.HIGH, KotlinUser("Ann")), todo)
        assertEquals("POST", server.request.method)
        assertEquals(listOf("graphql"), server.request.path)
        assertEquals(listOf("application/json"), server.request.headers["Content-Type"])
        assertEquals(
            mapOf(
                "query" to "query QueryTodo(\$id: ID!) { result: todo(id: \$id) { id title done priority owner { name } } }",
                "variables" to mapOf("id" to "1"),
            ),
            server.json(),
        )
    }

    @Test
    fun `Kotlin client reads a null result`() = runBlocking {
        assertNull(KotlinServer("""{"data":{"result":null}}""").client().queryTodo(KotlinID("1")))
    }

    @Test
    fun `Kotlin client sends input objects and takes a query without variables`() = runBlocking {
        val server = KotlinServer("""{"data":{"result":{"id":"2","title":"Laundry","done":false,"priority":null,"owner":{"name":"Ann"}}}}""")

        server.client().addTodo(KotlinNewTodo("Laundry", KotlinPriority.LOW))

        assertEquals(mapOf("input" to mapOf("title" to "Laundry", "priority" to "LOW")), server.json()["variables"])
        assertEquals("mutation ClearTodos { result: clearTodos }", KotlinServer("""{"data":{"result":true}}""").also { it.client().clearTodos() }.json()["query"])
    }

    @Test
    fun `Kotlin client fails on GraphQL errors`() {
        val error = assertFailsWith<IllegalStateException> {
            runBlocking { KotlinServer("""{"data":null,"errors":[{"message":"Not allowed","path":["todos"]}]}""").client().todos(null) }
        }
        assertEquals(
            "GraphQL request failed: [GraphQLError(message=Not allowed, locations=null, path=[todos], extensions=null)]",
            error.message,
        )
    }

    @Test
    fun `Kotlin client returns the first error of an rpc with an error type`() = runBlocking {
        val response = KotlinServer("""{"data":{"result":null},"errors":[{"message":"Unknown todo","path":["completeTodo"]}]}""")
            .client()
            .completeTodo(KotlinID("3"))

        assertEquals(KotlinCompleteTodo.Error(KotlinTodoError("Unknown todo")), response)
    }

    @Test
    fun `Java client posts the query and reads the result`() {
        val server = JavaServer("""{"data":{"result":[{"id":"1","title":"Groceries","done":true,"owner":{"name":"Ann"}}]}}""")

        val todos = server.client().todos(Optional.of(true)).join()

        assertEquals(listOf(JavaTodo(JavaID("1"), "Groceries", true, Optional.empty<JavaPriority>(), JavaUser("Ann"))), todos)
        assertEquals(
            mapOf(
                "query" to "query Todos(\$done: Boolean) { result: todos(done: \$done) { id title done priority owner { name } } }",
                "variables" to mapOf("done" to true),
            ),
            server.json(),
        )
    }

    @Test
    fun `Java client returns the first error of an rpc with an error type`() {
        val response = JavaServer("""{"errors":[{"message":"Unknown todo"}]}""").client().completeTodo(JavaID("3")).join()

        assertEquals(JavaCompleteTodo.Error(JavaTodoError("Unknown todo")), response)
    }

    @Test
    fun `Java client fails on a missing result`() {
        val error = assertFailsWith<java.util.concurrent.CompletionException> {
            JavaServer("""{"data":null}""").client().todos(Optional.empty()).join()
        }
        assertEquals("GraphQL response without data for Todos", error.cause?.message)
    }

    @Test
    fun `Kotlin client sends the selected fields with the variables`() = runBlocking {
        val server = KotlinServer("""{"data":{"result":{"id":"1","title":"Groceries","owner":{"name":"Ann"}}}}""")

        server.client().queryTodo(KotlinID("1")) {
            id()
            title()
            owner {
                name()
            }
        }

        assertEquals(mapOf("query" to "query QueryTodo(\$id: ID!) { result: todo(id: \$id) { id title owner { name } } }", "variables" to mapOf("id" to "1")), server.json())
    }

    @Test
    fun `Kotlin client reads the selected fields into the partial result`() = runBlocking {
        val server = KotlinServer("""{"data":{"result":{"title":"Groceries","owner":{"name":"Ann"}}}}""")

        val todo: KotlinTodoResult? = server.client().queryTodo(KotlinID("1")) {
            title()
            owner { name() }
        }

        assertEquals(KotlinTodoResult(id = null, title = "Groceries", done = null, priority = null, owner = KotlinUserResult(name = "Ann")), todo)
        assertEquals("query QueryTodo(\$id: ID!) { result: todo(id: \$id) { title owner { name } } }", server.json()["query"])
    }

    @Test
    fun `Kotlin client reads a union into the partial result with its typename`() = runBlocking {
        val server = KotlinServer("""{"data":{"result":[{"__typename":"Tag","name":"home"},{"__typename":"Note","text":"Milk"}]}}""")

        val results = server.client().search("a") {
            onNote { text() }
            onTag { name() }
        }

        assertEquals(listOf("Tag" to "home", "Note" to "Milk"), results.map { it.__typename to (it.name ?: it.text) })
        assertEquals("query Search(\$text: String!) { result: search(text: \$text) { __typename ... on Note { text } ... on Tag { name } } }", server.json()["query"])
    }

    @Test
    fun `Kotlin client fails a selected call on GraphQL errors, also with an error type`() {
        val error = assertFailsWith<IllegalStateException> {
            runBlocking { KotlinServer("""{"errors":[{"message":"Unknown todo"}]}""").client().completeTodo(KotlinID("3")) { id() } }
        }
        assertEquals("GraphQL request failed: [TodoError(message=Unknown todo)]", error.message)
    }

    @Test
    fun `Java client reads the selected fields into the partial result`() {
        val server = JavaServer("""{"data":{"result":[{"id":"1","owner":{"name":"Ann"}}]}}""")

        val todos: List<JavaTodoResult> = server.client().todos(Optional.of(true)) { todo -> todo.id().owner { owner -> owner.name() } }.join()

        assertEquals(
            listOf(JavaTodoResult(Optional.of(JavaID("1")), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(JavaUserResult(Optional.of("Ann"))))),
            todos,
        )
        assertEquals(mapOf("query" to "query Todos(\$done: Boolean) { result: todos(done: \$done) { id owner { name } } }", "variables" to mapOf("done" to true)), server.json())
    }

    @Test
    fun `Scala client posts the query and reads the full result`() {
        assertEquals("query QueryTodo(\$id: ID!) { result: todo(id: \$id) { id title done priority owner { name } } }", scalaCheck("queriesTheFullResult"))
    }

    @Test
    fun `Scala client reads the selected fields into the partial result`() {
        assertEquals("query QueryTodo(\$id: ID!) { result: todo(id: \$id) { title owner { name } } }", scalaCheck("readsTheSelectedFields"))
    }

    @Test
    fun `Scala client returns the first error of an rpc with an error type`() {
        scalaCheck("returnsTheTypedError")
    }

    @Test
    fun `Scala client reads a union, whose members share its file`() {
        assertEquals("query Search(\$text: String!) { result: search(text: \$text) { __typename ... on Note { text } ... on Tag { name } } }", scalaCheck("readsAUnion"))
        assertEquals("query Search(\$text: String!) { result: search(text: \$text) { __typename ... on Note { text } ... on Tag { name } } }", scalaCheck("readsAUnionSelection"))
    }

    @Test
    fun `Only rpcs marked as a query or a mutation get a client`() {
        listOf("TodoAddedGraphQL", "CountTodosGraphQL").forEach { client ->
            assertFailsWith<ClassNotFoundException> {
                Class.forName("community.flock.wirespec.integration.graphql.kotlin.generated.graphql.$client")
            }
        }
        assertEquals(
            listOf("QueryTodo", "Todos", "Search", "AddTodo", "CompleteTodo", "ClearTodos").map { "$it.Service" },
            KotlinGraphQLClient::class.java.interfaces.map { "${it.enclosingClass.simpleName}.${it.simpleName}" },
        )
    }

    @Test
    fun `Each rpc gets one file that bundles its client, envelopes, selections and results`() {
        val ast = object : ParseContext, NoLogger {
            override val spec = WirespecSpec
        }.parse(
            nonEmptyListOf(
                ModuleContent(
                    FileUri(""),
                    """
                    type Todo { id: String, owner: User }
                    type User { name: String }
                    @GraphQLQuery
                    rpc GetTodo { id: String } -> Todo
                    @GraphQLQuery
                    rpc ListTodos {} -> Todo[]
                    """.trimIndent(),
                ),
            ),
        ).getOrNull()!!
        val emitter = KotlinEmitter(PackageName("community.flock.wirespec.generated"), EmitShared(false))

        val files = emitter.applyExtensions(nonEmptyListOf(GraphQLClientExtension(emitter.packageName, FileExtension.Kotlin))).emit(ast, noLogger)
            .filter { "/graphql/" in it.file }
            .associate { it.file.substringAfterLast("/") to it.result }

        assertEquals(setOf("GetTodoGraphQL.kt", "ListTodosGraphQL.kt", "GraphQLClient.kt"), files.keys)
        listOf("data class GraphQLClient(", "data class GraphQLResponse(", "data class GraphQLError(", "data class TodoSelection(", "data class UserResult(")
            .forEach { assertTrue(it in files.getValue("GetTodoGraphQL.kt"), "GetTodoGraphQL.kt misses $it") }
    }

    @Test
    fun `Other languages are left untouched`() {
        val ast = object : ParseContext, NoLogger {
            override val spec = WirespecSpec
        }.parse(nonEmptyListOf(ModuleContent(FileUri(""), "type Todo { id: String }\nrpc GetTodo { id: String } -> Todo"))).getOrNull()!!
        val emitter = KotlinEmitter(PackageName("community.flock.wirespec.generated"), EmitShared(false))

        assertEquals(
            emitter.emit(ast, noLogger).toList(),
            emitter.applyExtensions(nonEmptyListOf(GraphQLClientExtension(emitter.packageName, FileExtension.TypeScript))).emit(ast, noLogger).toList(),
        )
    }

    // Scala compiles after the Kotlin tests, so the Scala side is reached reflectively.
    private fun scalaCheck(name: String): Any? = Class.forName("community.flock.wirespec.integration.graphql.extension.ScalaGraphQLCheck").getMethod(name).invoke(null)

    private inner class KotlinServer(private val response: String) : KotlinWirespec.Transportation {
        lateinit var request: KotlinWirespec.RawRequest

        fun client(): KotlinGraphQLClient = KotlinGraphQLClient(KotlinSerialization(objectMapper), this)

        fun json(): Map<*, *> = objectMapper.readValue(request.body, Map::class.java)

        override suspend fun transport(request: KotlinWirespec.RawRequest): KotlinWirespec.RawResponse = KotlinWirespec.RawResponse(200, emptyMap(), response.toByteArray())
            .also { this.request = request }
    }

    private inner class JavaServer(private val response: String) : JavaWirespec.Transportation {
        lateinit var request: JavaWirespec.RawRequest

        fun client(): JavaGraphQLClient = JavaGraphQLClient(JavaSerialization(objectMapper), this)

        fun json(): Map<*, *> = objectMapper.readValue(request.body().get(), Map::class.java)

        override fun transport(request: JavaWirespec.RawRequest): CompletableFuture<JavaWirespec.RawResponse> = CompletableFuture.completedFuture(JavaWirespec.RawResponse(200, emptyMap(), Optional.of(response.toByteArray())))
            .also { this.request = request }
    }
}
