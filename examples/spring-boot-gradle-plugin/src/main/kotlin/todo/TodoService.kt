package community.flock.wirespec.examples.app.todo

import org.springframework.stereotype.Service

@Service
class TodoService(private val repository: TodoRepository) {

    fun getAllTodos() = repository.getAllTodos()

    fun getTodoById(id: Todo.Id) = repository.getTodoById(id)

    fun saveTodo(todo: Todo) = repository.saveTodo(todo)

    fun deleteTodoById(id: Todo.Id) = repository.deleteTodoById(id)

}
