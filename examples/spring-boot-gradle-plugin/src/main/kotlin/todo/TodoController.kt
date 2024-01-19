package community.flock.wirespec.examples.app.todo

import community.flock.wirespec.examples.app.exception.AppException.TodoNotFoundException
import community.flock.wirespec.generated.kotlin.Todo
import community.flock.wirespec.generated.kotlin.TodoId
import community.flock.wirespec.generated.kotlin.TodoInput
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/todos")
class TodoController(private val repository: TodoRepository) {

    @GetMapping
    fun getAllTodos(): List<Todo> = TodoRepository.getAllTodos()

    @GetMapping("/{id}")
    fun getTodoById(@PathVariable id: String): Todo = TodoId(id).let(repository::getTodoById)
        ?: throw TodoNotFoundException(id)

    @PostMapping
    fun postTodo(@RequestBody input: TodoInput): Todo = input.consume().let(repository::saveTodo)

    @DeleteMapping("/{id}")
    fun deleteTodoById(@PathVariable id: String): Todo = TodoRepository.deleteTodoById(TodoId(id))
        ?: throw TodoNotFoundException(id)

    private fun TodoInput.consume(): Todo = Todo(
        id = TodoId(value = UUID.randomUUID().toString()),
        name = name,
        done = done,
    )
}
