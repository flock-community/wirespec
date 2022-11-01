package community.flock.wirespec.examples.todo_app

import community.flock.wirespec.generated.Todo
import community.flock.wirespec.generated.TodoId
import community.flock.wirespec.generated.TodoInput
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/todos")
class TodoController {

    private val todos = mutableListOf<Todo>()

    @GetMapping("/")
    fun list(): List<Todo> = todos

    @GetMapping("/:id")
    fun get(@PathVariable id: String) = todos.find { it.id.value == id }

    @PostMapping("/")
    fun post(input: TodoInput) =
        Todo(
            id = TodoId(value = UUID.randomUUID().toString()),
            name = input.name,
            done = input.done,
        ).run { todos.add(this) }
}