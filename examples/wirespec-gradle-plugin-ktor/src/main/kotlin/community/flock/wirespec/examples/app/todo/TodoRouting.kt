package community.flock.wirespec.examples.app.todo

import community.flock.wirespec.generated.kotlin.DeleteTodoById
import community.flock.wirespec.generated.kotlin.GetTodoById
import community.flock.wirespec.generated.kotlin.GetTodos
import community.flock.wirespec.generated.kotlin.PostTodo
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
        get(GetTodos.Endpoint.PATH_TEMPLATE) {
            handler
                .getTodos(GetTodos.Endpoint.Request(Unit))
                .body
                .let { call.respond(it) }
        }

        get(GetTodoById.Endpoint.PATH_TEMPLATE) {
            val id = call.parameters["id"]!!
            handler
                .getTodoById(GetTodoById.Endpoint.Request(TodoId(id), Unit))
                .body
                .let { call.respond(it) }
        }

        post(PostTodo.Endpoint.PATH_TEMPLATE) {
            val todo = call.receive<PotentialTodoDto>()
            handler
                .postTodo(PostTodo.Endpoint.Request(todo))
                .body
                .let { call.respond(it) }
        }

        delete(DeleteTodoById.Endpoint.PATH_TEMPLATE) {
            val id = call.parameters["id"]!!
            handler
                .deleteTodoById(DeleteTodoById.Endpoint.Request(TodoId(id), Unit))
                .body
                .let { call.respond(it) }
        }
    }
}
