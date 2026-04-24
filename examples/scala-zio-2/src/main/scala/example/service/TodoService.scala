package example.service

import community.flock.wirespec.generated.endpoint.{DeleteTodoById, GetTodoById, GetTodos, PostTodo}
import community.flock.wirespec.generated.model.*
import example.port.TodoRepository
import zio.*

import java.util.UUID

class TodoService(repo: TodoRepository)
  extends GetTodos.Handler[Task]
    with PostTodo.Handler[Task]
    with GetTodoById.Handler[Task]
    with DeleteTodoById.Handler[Task]:

  override def getTodos(request: GetTodos.Request): Task[GetTodos.Response[?]] =
    repo.findAll().map { todos =>
      val list = todos.filter(t => request.queries.done.forall(_ == t.done))
      new GetTodos.Response200(xTotal = list.size.toLong, body = list)
    }

  override def postTodo(request: PostTodo.Request): Task[PostTodo.Response[?]] =
    for
      id  <- ZIO.succeed(TodoId(UUID.randomUUID().toString))
      dto  = TodoDto(
               id       = id,
               name     = request.body.name,
               done     = request.body.done,
               testInt0 = request.body.testInt0,
               testInt1 = request.body.testInt1,
               testInt2 = request.body.testInt2,
               testNum0 = request.body.testNum0,
               testNum1 = request.body.testNum1,
               testNum2 = request.body.testNum2,
             )
      saved <- repo.save(dto)
    yield new PostTodo.Response200(saved)

  override def getTodoById(request: GetTodoById.Request): Task[GetTodoById.Response[?]] =
    repo.findById(request.path.id).map {
      case Some(todo) => new GetTodoById.Response200(todo)
      case None       => new GetTodoById.Response404(TodoError(404, s"Todo ${request.path.id.value} not found"))
    }

  override def deleteTodoById(request: DeleteTodoById.Request): Task[DeleteTodoById.Response[?]] =
    repo.delete(request.path.id).map {
      case Some(todo) => new DeleteTodoById.Response200(todo)
      case None       => new DeleteTodoById.Response404(TodoError(404, s"Todo ${request.path.id.value} not found"))
    }

object TodoService:
  val layer: URLayer[
    TodoRepository,
    GetTodos.Handler[Task]
      & PostTodo.Handler[Task]
      & GetTodoById.Handler[Task]
      & DeleteTodoById.Handler[Task]
  ] =
    ZLayer.fromZIO(ZIO.service[TodoRepository].map(new TodoService(_)))
