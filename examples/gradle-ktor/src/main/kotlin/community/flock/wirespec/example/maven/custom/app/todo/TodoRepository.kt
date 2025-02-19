package community.flock.wirespec.example.maven.custom.app.todo

interface HasTodoRepository {
    val todoRepository: TodoRepository
}

interface TodoRepository {
    fun getAllTodos(): List<Todo>

    fun getTodoById(id: Todo.Id): Todo

    fun saveTodo(todo: Todo): Todo

    fun deleteTodoById(id: Todo.Id): Todo
}
