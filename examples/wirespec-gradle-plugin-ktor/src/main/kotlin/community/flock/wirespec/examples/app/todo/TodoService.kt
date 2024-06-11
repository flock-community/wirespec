package community.flock.wirespec.examples.app.todo

interface TodoContext : HasTodoRepository

object TodoService {

    fun TodoContext.getAllTodos() = todoRepository.getAllTodos()

    fun TodoContext.getTodoById(id: Todo.Id) = todoRepository.getTodoById(id)

    fun TodoContext.saveTodo(todo: Todo) = todoRepository.saveTodo(todo)

    fun TodoContext.deleteTodoById(id: Todo.Id) = todoRepository.deleteTodoById(id)

}
