package community.flock.wirespec.examples.todo_app

import community.flock.wirespec.generated.Todo
import community.flock.wirespec.generated.TodoId
import community.flock.wirespec.generated.validate
import community.flock.wirespec.generated.TodoInput
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@RestController
@RequestMapping("/todos")
class TodoController {

    private val todos = mutableListOf<Todo>()

    @GetMapping
    fun getAll(): List<Todo> = todos

    @GetMapping("/{id}")
    fun getById(@PathVariable id: String) = todos.find { id == it.id.value }

    @PostMapping
    fun post(@RequestBody input: TodoInput) = Todo(
        id = TodoId(value = UUID.randomUUID().toString()),
        name = input.name,
        done = input.done,
    ).also(todos::add)
}
