package community.flock.wirespec.example.maven.custom.app.todo

import community.flock.wirespec.example.maven.custom.app.common.Serialization
import community.flock.wirespec.generated.kotlin.endpoint.DeleteTodoById
import community.flock.wirespec.generated.kotlin.endpoint.GetTodoById
import community.flock.wirespec.generated.kotlin.endpoint.GetTodos
import community.flock.wirespec.generated.kotlin.endpoint.PostTodo
import community.flock.wirespec.kotlin.Wirespec
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.toMap

fun Application.todoModule(handler: TodoHandler) {
    routing {
        with(GetTodos.Handler) {
            route(pathTemplate, method.let(HttpMethod::parse)) {
                handle {
                    call.request.toRawRequest()
                        .let(server(Serialization)::from)
                        .let { handler.getTodos(it) }
                        .run { call.respond(body) }
                }
            }
        }

        with(GetTodoById.Handler) {
            route(pathTemplate, method.let(HttpMethod::parse)) {
                handle {
                    call.request.toRawRequest()
                        .let(server(Serialization)::from)
                        .let { handler.getTodoById(it) }
                        .run { call.respond(body) }
                }
            }
        }

        with(PostTodo.Handler) {
            route(pathTemplate, method.let(HttpMethod::parse)) {
                handle {
                    call.request.toRawRequest()
                        .let(server(Serialization)::from)
                        .let { handler.postTodo(it) }
                        .run { call.respond(body) }
                }
            }
        }

        with(DeleteTodoById.Handler) {
            route(pathTemplate, method.let(HttpMethod::parse)) {
                handle {
                    call.request.toRawRequest()
                        .let(server(Serialization)::from)
                        .let { handler.deleteTodoById(it) }
                        .run { call.respond(body) }
                }
            }
        }
    }
}

suspend fun ApplicationRequest.toRawRequest() = Wirespec.RawRequest(
    method = httpMethod.value,
    path = path().split("/").filter { it.isNotBlank() },
    queries = queryParameters.toMap(),
    headers = headers.toMap(),
    body = call.receive<String>(),
)
