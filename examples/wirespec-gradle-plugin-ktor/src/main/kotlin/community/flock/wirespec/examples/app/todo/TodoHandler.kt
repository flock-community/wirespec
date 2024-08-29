package community.flock.wirespec.examples.app.todo

import community.flock.wirespec.examples.app.exception.TodoIdNotValidException
import community.flock.wirespec.examples.app.todo.TodoConsumer.consume
import community.flock.wirespec.examples.app.todo.TodoProducer.produce
import community.flock.wirespec.examples.app.todo.TodoService.deleteTodoById
import community.flock.wirespec.examples.app.todo.TodoService.getAllTodos
import community.flock.wirespec.examples.app.todo.TodoService.getTodoById
import community.flock.wirespec.examples.app.todo.TodoService.saveTodo
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

    private val context = object : TodoContext {
        override val todoRepository = liveTodoRepository
    }

    override suspend fun getTodos(request: GetTodosEndpoint.Request<*>) = when (request) {
        is GetTodosEndpoint.RequestUnit -> context.getAllTodos()
            .map { it.produce() }
            .let(GetTodosEndpoint::Response200ApplicationJson)
    }

    override suspend fun getTodoById(request: GetTodoByIdEndpoint.Request<*>) = when (request) {
        is GetTodoByIdEndpoint.RequestUnit -> request.path.id
            .also { if (!it.validate()) throw TodoIdNotValidException(it.value) }
            .let { Todo.Id(it.value) }
            .let { context.getTodoById(it) }
            .produce()
            .let(GetTodoByIdEndpoint::Response200ApplicationJson)
    }

    override suspend fun postTodo(request: PostTodoEndpoint.Request<*>) = when (request) {
        is PostTodoEndpoint.RequestApplicationJson -> request.body
            .consume()
            .let { context.saveTodo(it) }
            .produce()
            .let(PostTodoEndpoint::Response200ApplicationJson)
    }

    override suspend fun deleteTodoById(request: DeleteTodoByIdEndpoint.Request<*>) = when (request) {
        is DeleteTodoByIdEndpoint.RequestUnit -> request.path.id
            .also { if (!it.validate()) throw TodoIdNotValidException(it.value) }
            .let { Todo.Id(it.value) }
            .let { context.deleteTodoById(it) }
            .produce()
            .let(DeleteTodoByIdEndpoint::Response200ApplicationJson)
    }
}
