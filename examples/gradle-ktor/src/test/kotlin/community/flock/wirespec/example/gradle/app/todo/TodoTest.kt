package community.flock.wirespec.example.gradle.app.todo

import org.junit.Test
import kotlin.test.assertTrue

interface TestContext {
    val todoService: TodoService
}

class TodoTest {
    @Test
    fun testTodo() = testContext {
        assertTrue { todoService.getAllTodos().isNotEmpty() }
    }
}

private fun testContext(test: TestContext.() -> Unit) = object : TestContext {
    override val todoService = object :
        TodoService {
        override val todoRepository = LiveTodoRepository()
    }
}.test()
