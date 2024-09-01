package community.flock.wirespec.examples.app.todo

import community.flock.wirespec.examples.app.exception.TodoNotFoundException

class LiveTodoRepository : TodoRepository {

    private val todos = mutableListOf(
        Todo(
            id = Todo.Id("8132b795-143f-4afb-8c8a-0608cb63c79c"),
            name = Name("Name"),
            done = true,
        )
    )

    override fun getAllTodos(): List<Todo> = todos

    override fun getTodoById(id: Todo.Id): Todo = todos.find { it.id == id } ?: throw TodoNotFoundException(id)

    override fun saveTodo(todo: Todo): Todo = todo.also(todos::add)

    override fun deleteTodoById(id: Todo.Id): Todo = getTodoById(id).also(todos::remove)

}
