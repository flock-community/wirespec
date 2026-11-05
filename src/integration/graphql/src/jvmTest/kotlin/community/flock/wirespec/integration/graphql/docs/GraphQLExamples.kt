// The imports the docs show come first, between region comments, so they are not in lexicographic order.
@file:Suppress("ktlint:standard:import-ordering")

package community.flock.wirespec.integration.graphql.docs

// The examples of the GraphQL client docs page, which shows the regions between the region comments.

// region create-client
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import community.flock.wirespec.integration.jackson.v2.kotlin.WirespecSerialization
import community.flock.wirespec.integration.jvm.transport.HttpTransportation
// endregion create-client
import community.flock.wirespec.integration.graphql.kotlin.generated.graphql.GraphQLClient
import community.flock.wirespec.integration.graphql.kotlin.generated.graphql.QueryTodoClient
import community.flock.wirespec.integration.graphql.kotlin.generated.graphql.QueryTodoGraphQL
import community.flock.wirespec.integration.graphql.kotlin.generated.graphql.SearchResultResult
import community.flock.wirespec.integration.graphql.kotlin.generated.graphql.TodoResult
import community.flock.wirespec.integration.graphql.kotlin.generated.graphql.graphQL
import community.flock.wirespec.integration.graphql.kotlin.generated.model.ID
import community.flock.wirespec.integration.graphql.kotlin.generated.model.NewTodo
import community.flock.wirespec.integration.graphql.kotlin.generated.model.Priority
import community.flock.wirespec.integration.graphql.kotlin.generated.model.Todo
import community.flock.wirespec.integration.graphql.kotlin.generated.rpc.CompleteTodo
import community.flock.wirespec.integration.graphql.kotlin.generated.rpc.QueryTodo
import community.flock.wirespec.kotlin.Wirespec

object GraphQLExamples {

    fun createClient(): GraphQLClient {
        // region create-client
        val serialization = WirespecSerialization(
            ObjectMapper()
                .registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false),
        )

        val client = GraphQLClient(serialization, HttpTransportation("https://api.example.com"))
        // endregion create-client
        return client
    }

    fun operationClient(serialization: Wirespec.Serialization, transportation: Wirespec.Transportation): QueryTodo.Service {
        // region operation-client
        val queryTodo: QueryTodo.Service = QueryTodoClient(serialization, transportation)
        // endregion operation-client
        return queryTodo
    }

    suspend fun calls(client: GraphQLClient): List<Any?> {
        // region calls
        val todo: Todo? = client.queryTodo(ID("1"))

        val open: List<Todo> = client.todos(done = false)

        val added: Todo = client.addTodo(NewTodo(title = "Groceries", priority = Priority.HIGH))

        client.clearTodos()
        // endregion calls
        return listOf(todo, open, added)
    }

    suspend fun errors(client: GraphQLClient): String {
        // region errors
        val message = when (val response = client.completeTodo(ID("1"))) {
            is CompleteTodo.Result -> "Done: ${response.value.title}"
            is CompleteTodo.Error -> "Failed: ${response.value.message}"
        }
        // endregion errors
        return message
    }

    suspend fun selection(client: GraphQLClient): List<Any?> {
        // region selection
        val todo: TodoResult? = client.queryTodo(ID("1")) {
            title()
            owner {
                name()
            }
        }

        val title: String? = todo?.title
        val owner: String? = todo?.owner?.name
        val done: Boolean? = todo?.done // null: not selected
        // endregion selection
        return listOf(title, owner, done)
    }

    suspend fun union(client: GraphQLClient): List<String?> {
        // region union
        val results: List<SearchResultResult> = client.search("milk") {
            onNote { text() }
            onTag { name() }
        }

        val kinds: List<String?> = results.map { it.__typename } // "Note" or "Tag"
        // endregion union
        return kinds
    }

    fun request(): QueryTodoGraphQL.GraphQLRequest {
        // region request
        val request = QueryTodo.graphQL.request(ID("1")) {
            title()
        }

        val query: String = request.query // query QueryTodo($id: ID!) { result: todo(id: $id) { title } }
        val id: ID = request.variables.id
        // endregion request
        return request.takeIf { query.isNotEmpty() && id == ID("1") } ?: error("Unexpected request")
    }

    fun path(http: HttpTransportation): Wirespec.Transportation {
        // region path
        val transportation = Wirespec.Transportation { request -> http.transport(request.copy(path = listOf("api", "graphql"))) }
        // endregion path
        return transportation
    }
}
