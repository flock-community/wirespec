package community.flock.wirespec.example.maven.custom.app.todo

import community.flock.wirespec.example.maven.custom.app.exception.TodoIdNotValidException
import community.flock.wirespec.example.maven.custom.app.todo.TodoConsumer.consume
import community.flock.wirespec.example.maven.custom.app.todo.TodoProducer.produce
import community.flock.wirespec.generated.kotlin.endpoint.DeleteTodoById
import community.flock.wirespec.generated.kotlin.endpoint.GetTodoById
import community.flock.wirespec.generated.kotlin.endpoint.GetTodos
import community.flock.wirespec.generated.kotlin.endpoint.PostTodo
import community.flock.wirespec.generated.kotlin.model.validate

private interface TodoApi :
    GetTodos.Handler,
    GetTodoById.Handler,
    PostTodo.Handler,
    DeleteTodoById.Handler

class TodoHandler(
    liveTodoRepository: TodoRepository,
) : TodoApi {
    private val service =
        object : TodoService {
            override val todoRepository = liveTodoRepository
        }

    override suspend fun getTodos(request: GetTodos.Request): GetTodos.Response200 = service
        .getAllTodos()
        .map { it.produce() }
        .let(GetTodos::Response200)

    override suspend fun getTodoById(request: GetTodoById.Request): GetTodoById.Response200 = request.path.id
        .also { if (!it.validate()) throw TodoIdNotValidException(invalidId = it.value) }
        .value
        .let(Todo::Id)
        .let(service::getTodoById)
        .produce()
        .let(GetTodoById::Response200)

    override suspend fun postTodo(request: PostTodo.Request): PostTodo.Response200 = request.body
        .consume()
        .let(service::saveTodo)
        .produce()
        .let(PostTodo::Response200)

    override suspend fun deleteTodoById(request: DeleteTodoById.Request): DeleteTodoById.Response200 = request.path.id
        .also { if (!it.validate()) throw TodoIdNotValidException(invalidId = it.value) }
        .value
        .let(Todo::Id)
        .let(service::deleteTodoById)
        .produce()
        .let(DeleteTodoById::Response200)
}
