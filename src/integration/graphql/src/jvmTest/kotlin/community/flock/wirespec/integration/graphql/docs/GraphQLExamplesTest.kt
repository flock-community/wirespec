package community.flock.wirespec.integration.graphql.docs

import kotlin.test.Test
import kotlin.test.assertEquals

// The docs page shows the examples, so they compile with the module's tests; these run the ones that need no server.
class GraphQLExamplesTest {

    private val query = "query QueryTodo(\$id: ID!) { result: todo(id: \$id) { title } }"

    @Test
    fun `Kotlin request example writes the query it shows`() {
        assertEquals(query, GraphQLExamples.request().query)
    }

    @Test
    fun `Java request example writes the query it shows`() {
        assertEquals(query, JavaGraphQLExamples.request().query())
    }

    @Test
    fun `Scala request example writes the query it shows`() {
        val request = Class.forName("community.flock.wirespec.integration.graphql.docs.ScalaGraphQLExamples").getMethod("request").invoke(null)
        assertEquals(query, request.javaClass.getMethod("query").invoke(request))
    }
}
