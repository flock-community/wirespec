package community.flock.wirespec.example.maven.custom.app.todo

import community.flock.wirespec.example.maven.custom.app.exception.TodoIdNotValidException
import community.flock.wirespec.example.maven.custom.app.todo.TodoConsumer.consume
import community.flock.wirespec.example.maven.custom.app.todo.TodoProducer.produce
import community.flock.wirespec.generated.kotlin.DeleteTodoByIdEndpoint
import community.flock.wirespec.generated.kotlin.GetTodoByIdEndpoint
import community.flock.wirespec.generated.kotlin.GetTodosEndpoint
import community.flock.wirespec.generated.kotlin.PostTodoEndpoint
import community.flock.wirespec.generated.kotlin.validate

private interface TodoApi :
    GetTodosEndpoint.Handler,
    GetTodoByIdEndpoint.Handler,
    PostTodoEndpoint.Handler,
    DeleteTodoByIdEndpoint.Handler

class TodoHandler(liveTodoRepository: TodoRepository) : TodoApi {

    private val service = object : TodoService {
        override val todoRepository = liveTodoRepository
    }

    override suspend fun getTodos(request: GetTodosEndpoint.Request): GetTodosEndpoint.Response200 =
        service.getAllTodos()
            .map { it.produce() }
            .let(GetTodosEndpoint::Response200)

    override suspend fun getTodoById(request: GetTodoByIdEndpoint.Request): GetTodoByIdEndpoint.Response200 =
        request.path.id
            .also { if (!it.validate()) throw TodoIdNotValidException(
                it.value
            )
            }
            .value
            .let(Todo::Id)
            .let(service::getTodoById)
            .produce()
            .let(GetTodoByIdEndpoint::Response200)

    override suspend fun postTodo(request: PostTodoEndpoint.Request): PostTodoEndpoint.Response200 =
        request.body
            .consume()
            .let(service::saveTodo)
            .produce()
            .let(PostTodoEndpoint::Response200)

    override suspend fun deleteTodoById(request: DeleteTodoByIdEndpoint.Request): DeleteTodoByIdEndpoint.Response200 =
        request.path.id
            .also { if (!it.validate()) throw TodoIdNotValidException(
                it.value
            )
            }
            .value
            .let(Todo::Id)
            .let(service::deleteTodoById)
            .produce()
            .let(DeleteTodoByIdEndpoint::Response200)
}
