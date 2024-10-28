package community.flock.wirespec.example.maven.custom.app

import community.flock.wirespec.example.maven.custom.app.todo.LiveTodoRepository
import community.flock.wirespec.example.maven.custom.app.todo.todoModule
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            config()
            todoModule(LiveTodoRepository())
        }

        client.get("/api/todos").apply {
            val expected = """[{"id":{"value":"8132b795-143f-4afb-8c8a-0608cb63c79c"},"name":"Name","done":true}]"""
            assertEquals(HttpStatusCode.OK, status)
            assertEquals(expected, bodyAsText())
        }
    }
}
