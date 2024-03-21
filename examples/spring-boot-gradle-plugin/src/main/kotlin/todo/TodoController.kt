package community.flock.wirespec.examples.app.todo

import community.flock.wirespec.generated.kotlin.DeleteTodoByIdEndpoint
import community.flock.wirespec.generated.kotlin.GetTodoByIdEndpoint
import community.flock.wirespec.generated.kotlin.GetTodosEndpoint
import community.flock.wirespec.generated.kotlin.PostTodoEndpoint
import community.flock.wirespec.generated.kotlin.PotentialTodoDto
import community.flock.wirespec.generated.kotlin.TodoId
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TodoController(private val handler: TodoHandler) {

    @GetMapping(GetTodosEndpoint.PATH)
    suspend fun getAllTodos() = handler
        .getTodos(GetTodosEndpoint.RequestUnit()).content.body

    @GetMapping(GetTodoByIdEndpoint.PATH)
    suspend fun getTodoById(@PathVariable id: String) = handler
        .getTodoById(GetTodoByIdEndpoint.RequestUnit(id = TodoId(value = id))).content.body

    @PostMapping(PostTodoEndpoint.PATH)
    suspend fun postTodo(@RequestBody input: PotentialTodoDto) = handler
        .postTodo(PostTodoEndpoint.RequestApplicationJson(input)).content.body

    @DeleteMapping(DeleteTodoByIdEndpoint.PATH)
    suspend fun deleteTodoById(@PathVariable id: String) = handler
        .deleteTodoById(DeleteTodoByIdEndpoint.RequestUnit(TodoId(id))).content.body

}
