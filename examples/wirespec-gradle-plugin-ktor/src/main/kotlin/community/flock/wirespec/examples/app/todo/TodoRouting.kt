package community.flock.wirespec.examples.app.todo

import community.flock.wirespec.generated.kotlin.DeleteTodoByIdEndpoint
import community.flock.wirespec.generated.kotlin.GetTodoByIdEndpoint
import community.flock.wirespec.generated.kotlin.GetTodosEndpoint
import community.flock.wirespec.generated.kotlin.PostTodoEndpoint
import community.flock.wirespec.generated.kotlin.PotentialTodoDto
import community.flock.wirespec.generated.kotlin.TodoId
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.todoModule(todoRepository: TodoRepository) {

    val handler = TodoHandler(todoRepository)

    routing {
        get(GetTodosEndpoint.PATH_TEMPLATE) {
            handler
                .getTodos(GetTodosEndpoint.Request)
                .body
                .let { call.respond(it) }
        }

        get(GetTodoByIdEndpoint.PATH_TEMPLATE) {
            val id = call.parameters["id"]!!
            handler
                .getTodoById(GetTodoByIdEndpoint.Request(TodoId(id)))
                .body
                .let { call.respond(it) }
        }

        post(PostTodoEndpoint.PATH_TEMPLATE) {
            val todo = call.receive<PotentialTodoDto>()
            handler
                .postTodo(PostTodoEndpoint.Request(todo))
                .body
                .let { call.respond(it) }
        }

        delete(DeleteTodoByIdEndpoint.PATH_TEMPLATE) {
            val id = call.parameters["id"]!!
            handler
                .deleteTodoById(DeleteTodoByIdEndpoint.Request(TodoId(id)))
                .body
                .let { call.respond(it) }
        }
    }
}
