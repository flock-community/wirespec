package community.flock.wirespec.examples.app.todo

import community.flock.wirespec.examples.app.exception.TodoIdNotValidException
import community.flock.wirespec.examples.app.todo.TodoConsumer.consume
import community.flock.wirespec.examples.app.todo.TodoProducer.produce
import community.flock.wirespec.examples.app.todo.TodoService.deleteTodoById
import community.flock.wirespec.examples.app.todo.TodoService.getAllTodos
import community.flock.wirespec.examples.app.todo.TodoService.getTodoById
import community.flock.wirespec.examples.app.todo.TodoService.saveTodo
import community.flock.wirespec.generated.kotlin.DeleteTodoById
import community.flock.wirespec.generated.kotlin.GetTodoById
import community.flock.wirespec.generated.kotlin.GetTodos
import community.flock.wirespec.generated.kotlin.PostTodo
import community.flock.wirespec.generated.kotlin.validate

private interface TodoApi :
    GetTodos.Endpoint.Handler,
    GetTodoById.Endpoint.Handler,
    PostTodo.Endpoint.Handler,
    DeleteTodoById.Endpoint.Handler

class TodoHandler(liveTodoRepository: TodoRepository) : TodoApi {

    private val context = object : TodoContext {
        override val todoRepository = liveTodoRepository
    }

    override suspend fun getTodos(request: GetTodos.Endpoint.Request) =
        context.getAllTodos()
            .map { it.produce() }
            .let(GetTodos.Endpoint::Response200)


    override suspend fun getTodoById(request: GetTodoById.Endpoint.Request) =
        request.path.id
            .also { if (!it.validate()) throw TodoIdNotValidException(it.value) }
            .let { Todo.Id(it.value) }
            .let { context.getTodoById(it) }
            .produce()
            .let(GetTodoById.Endpoint::Response200)

    override suspend fun postTodo(request: PostTodo.Endpoint.Request) =
        request.body
            .consume()
            .let { context.saveTodo(it) }
            .produce()
            .let(PostTodo.Endpoint::Response200)

    override suspend fun deleteTodoById(request: DeleteTodoById.Endpoint.Request) =
        request.path.id
            .also { if (!it.validate()) throw TodoIdNotValidException(it.value) }
            .let { Todo.Id(it.value) }
            .let { context.deleteTodoById(it) }
            .produce()
            .let(DeleteTodoById.Endpoint::Response200)
}
