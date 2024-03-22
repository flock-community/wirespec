package community.flock.wirespec.examples.app.todo

import community.flock.wirespec.examples.app.exception.TodoIdNotValidException
import community.flock.wirespec.examples.app.todo.TodoConsumer.consume
import community.flock.wirespec.examples.app.todo.TodoProducer.produce
import community.flock.wirespec.generated.kotlin.DeleteTodoByIdEndpoint
import community.flock.wirespec.generated.kotlin.GetTodoByIdEndpoint
import community.flock.wirespec.generated.kotlin.GetTodosEndpoint
import community.flock.wirespec.generated.kotlin.PostTodoEndpoint
import community.flock.wirespec.generated.kotlin.TodoId
import community.flock.wirespec.generated.kotlin.validate
import org.springframework.stereotype.Component

private interface TodoApi :
    GetTodosEndpoint,
    GetTodoByIdEndpoint,
    PostTodoEndpoint,
    DeleteTodoByIdEndpoint

@Component
class TodoHandler(private val service: TodoService) : TodoApi {

    override suspend fun getTodos(request: GetTodosEndpoint.Request<*>): GetTodosEndpoint.Response<*> =
        when (request) {
            is GetTodosEndpoint.RequestUnit -> service.getAllTodos()
                .map { it.produce() }
                .let(GetTodosEndpoint::Response200ApplicationJson)
        }

    override suspend fun getTodoById(request: GetTodoByIdEndpoint.Request<*>): GetTodoByIdEndpoint.Response<*> =
        when (request) {
            is GetTodoByIdEndpoint.RequestUnit -> TodoId(request.path.split("/").last())
                .also { if (!it.validate()) throw TodoIdNotValidException(it.value) }
                .let { Todo.Id(it.value) }
                .let(service::getTodoById)
                .produce()
                .let(GetTodoByIdEndpoint::Response200ApplicationJson)
        }

    override suspend fun postTodo(request: PostTodoEndpoint.Request<*>): PostTodoEndpoint.Response<*> =
        when (request) {
            is PostTodoEndpoint.RequestApplicationJson -> request.content.body
                .consume()
                .let(service::saveTodo)
                .produce()
                .let(PostTodoEndpoint::Response200ApplicationJson)
        }

    override suspend fun deleteTodoById(request: DeleteTodoByIdEndpoint.Request<*>): DeleteTodoByIdEndpoint.Response<*> =
        when (request) {
            is DeleteTodoByIdEndpoint.RequestUnit -> TodoId(request.path.split("/").last())
                .also { if (!it.validate()) throw TodoIdNotValidException(it.value) }
                .let { Todo.Id(it.value) }
                .let(service::deleteTodoById)
                .produce()
                .let(DeleteTodoByIdEndpoint::Response200ApplicationJson)
        }
}
