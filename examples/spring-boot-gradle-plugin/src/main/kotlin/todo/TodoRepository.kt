package community.flock.wirespec.examples.app.todo

import community.flock.wirespec.examples.app.exception.TodoNotFoundException
import org.springframework.stereotype.Repository

@Repository
object TodoRepository {

    private val todos = mutableListOf(
        Todo(
            id = Todo.Id("8132b795-143f-4afb-8c8a-0608cb63c79c"),
            name = Name("Name"),
            done = true,
        )
    )

    fun getAllTodos(): List<Todo> = todos

    fun getTodoById(id: Todo.Id): Todo = todos.find { it.id == id } ?: throw TodoNotFoundException(id)

    fun saveTodo(todo: Todo): Todo = todo.also(todos::add)

    fun deleteTodoById(id: Todo.Id): Todo = getTodoById(id).also(todos::remove)

}
