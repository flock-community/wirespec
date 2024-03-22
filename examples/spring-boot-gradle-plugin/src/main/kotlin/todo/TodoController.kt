package community.flock.wirespec.examples.app.todo

import community.flock.wirespec.Wirespec
import community.flock.wirespec.generated.kotlin.DeleteTodoByIdEndpoint
import community.flock.wirespec.generated.kotlin.GetTodoByIdEndpoint
import community.flock.wirespec.generated.kotlin.GetTodosEndpoint
import community.flock.wirespec.generated.kotlin.PostTodoEndpoint
import community.flock.wirespec.generated.kotlin.PotentialTodoDto
import community.flock.wirespec.generated.kotlin.TodoId
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Controller
class TodoController(private val handler: TodoHandler) {

    @GetMapping(GetTodosEndpoint.PATH)
    suspend fun getAllTodos() = handle {
        getTodos(GetTodosEndpoint.RequestUnit())
    }

    @GetMapping(GetTodoByIdEndpoint.PATH)
    suspend fun getTodoById(@PathVariable id: String) = handle {
        getTodoById(GetTodoByIdEndpoint.RequestUnit(TodoId(id)))
    }

    @PostMapping(PostTodoEndpoint.PATH)
    suspend fun postTodo(@RequestBody input: PotentialTodoDto) = handle {
        postTodo(PostTodoEndpoint.RequestApplicationJson(input))
    }

    @DeleteMapping(DeleteTodoByIdEndpoint.PATH)
    suspend fun deleteTodoById(@PathVariable id: String) = handle {
        deleteTodoById(DeleteTodoByIdEndpoint.RequestUnit(TodoId(id)))
    }

    private suspend fun <T> handle(block: suspend TodoHandler.() -> Wirespec.Response<T>) = handler.block()
        .run { ResponseEntity.status(status).body(content?.body) }

}
