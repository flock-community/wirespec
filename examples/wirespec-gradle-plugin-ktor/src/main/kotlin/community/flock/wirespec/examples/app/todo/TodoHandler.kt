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

    override suspend fun getTodos(request: GetTodosEndpoint.Request): GetTodosEndpoint.Response200 =
        context.getAllTodos()
            .map { it.produce() }
            .let(GetTodosEndpoint::Response200)

    override suspend fun getTodoById(request: GetTodoByIdEndpoint.Request): GetTodoByIdEndpoint.Response200 =
        request.path.id
            .also { if (!it.validate()) throw TodoIdNotValidException(it.value) }
            .let { Todo.Id(it.value) }
            .let { context.getTodoById(it) }
            .produce()
            .let(GetTodoByIdEndpoint::Response200)

    override suspend fun postTodo(request: PostTodoEndpoint.Request): PostTodoEndpoint.Response200 =
        request.body
            .consume()
            .let { context.saveTodo(it) }
            .produce()
            .let(PostTodoEndpoint::Response200)

    override suspend fun deleteTodoById(request: DeleteTodoByIdEndpoint.Request): DeleteTodoByIdEndpoint.Response200 =
        request.path.id
            .also { if (!it.validate()) throw TodoIdNotValidException(it.value) }
            .let { Todo.Id(it.value) }
            .let { context.deleteTodoById(it) }
            .produce()
            .let(DeleteTodoByIdEndpoint::Response200)
}
