package community.flock.wirespec.example.gradle.app.todo

interface TodoService : HasTodoRepository

fun TodoService.getAllTodos() = todoRepository.getAllTodos()

fun TodoService.getTodoById(id: Todo.Id) = todoRepository.getTodoById(id)

fun TodoService.saveTodo(todo: Todo) = todoRepository.saveTodo(todo)

fun TodoService.deleteTodoById(id: Todo.Id) = todoRepository.deleteTodoById(id)
