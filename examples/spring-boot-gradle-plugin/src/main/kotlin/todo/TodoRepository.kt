package community.flock.wirespec.examples.app.todo

import community.flock.wirespec.generated.kotlin.Todo
import community.flock.wirespec.generated.kotlin.TodoId
import org.springframework.stereotype.Repository

@Repository
object TodoRepository {

    private val todos = mutableListOf(
        Todo(
            id = TodoId("8132b795-143f-4afb-8c8a-0608cb63c79c"),
            name = "Name",
            done = true,
        )
    )

    fun getAllTodos(): List<Todo> = todos

    fun getTodoById(id: TodoId): Todo? = todos.find { it.id == id }

    fun saveTodo(todo: Todo): Todo = todo.also(todos::add)

    fun deleteTodoById(id: TodoId): Todo? = getTodoById(id)?.also(todos::remove)

}
