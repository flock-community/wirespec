package community.flock.wirespec.examples.app.todo

import community.flock.wirespec.examples.app.common.Serialization
import community.flock.wirespec.generated.kotlin.DeleteTodoByIdEndpoint
import community.flock.wirespec.generated.kotlin.GetTodoByIdEndpoint
import community.flock.wirespec.generated.kotlin.GetTodosEndpoint
import community.flock.wirespec.generated.kotlin.PostTodoEndpoint
import community.flock.wirespec.kotlin.Wirespec
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.toMap

fun Application.todoModule(todoRepository: TodoRepository) {

    val handler = TodoHandler(todoRepository)

    routing {
        with(GetTodosEndpoint.Handler) {
            route(pathTemplate, method.let(HttpMethod::parse)) {
                handle {
                    call.toRawRequest()
                        .let(server(Serialization)::consume)
                        .let { handler.getTodos(it) }
                        .run { call.respond(body) }
                }
            }
        }

        with(GetTodoByIdEndpoint.Handler) {
            route(pathTemplate, method.let(HttpMethod::parse)) {
                handle {
                    call.toRawRequest()
                        .let(server(Serialization)::consume)
                        .let { handler.getTodoById(it) }
                        .run { call.respond(body) }
                }
            }
        }

        with(PostTodoEndpoint.Handler) {
            route(pathTemplate, method.let(HttpMethod::parse)) {
                handle {
                    call.toRawRequest()
                        .let(server(Serialization)::consume)
                        .let { handler.postTodo(it) }
                        .run { call.respond(body) }
                }
            }
        }

        with(DeleteTodoByIdEndpoint.Handler) {
            route(pathTemplate, method.let(HttpMethod::parse)) {
                handle {
                    call.toRawRequest()
                        .let(server(Serialization)::consume)
                        .let { handler.deleteTodoById(it) }
                        .run { call.respond(body) }
                }
            }
        }
    }
}

suspend fun ApplicationCall.toRawRequest() =
    Wirespec.RawRequest(
        method = request.httpMethod.value,
        path = request.path().split("/").filter { it.isNotBlank() },
        queries = request.queryParameters.toMap(),
        headers = request.headers.toMap(),
        body = receive<String>(),
    )
