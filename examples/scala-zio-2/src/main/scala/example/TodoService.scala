package example

import community.flock.wirespec.generated.endpoint.{DeleteTodoById, GetTodoById, GetTodos, GetUsers, PostTodo}
import community.flock.wirespec.generated.model.*
import zio.*

import java.util.UUID

class TodoService(store: Ref[Map[TodoId, TodoDto]])
  extends GetTodos.Handler[Task]
    with PostTodo.Handler[Task]
    with GetTodoById.Handler[Task]
    with DeleteTodoById.Handler[Task]
    with GetUsers.Handler[Task]:

  override def getTodos(request: GetTodos.Request): Task[GetTodos.Response[?]] =
    store.get.map { todos =>
      val list = todos.values.toList.filter(t => request.queries.done.forall(_ == t.done))
      new GetTodos.Response200(xTotal = list.size.toLong, body = list)
    }

  override def postTodo(request: PostTodo.Request): Task[PostTodo.Response[?]] =
    for
      id <- ZIO.succeed(TodoId(UUID.randomUUID().toString))
      dto = TodoDto(
        id        = id,
        name      = request.body.name,
        done      = request.body.done,
        testInt0  = request.body.testInt0,
        testInt1  = request.body.testInt1,
        testInt2  = request.body.testInt2,
        testNum0  = request.body.testNum0,
        testNum1  = request.body.testNum1,
        testNum2  = request.body.testNum2,
      )
      _ <- store.update(_ + (id -> dto))
    yield new PostTodo.Response200(dto)

  override def getTodoById(request: GetTodoById.Request): Task[GetTodoById.Response[?]] =
    store.get.map { todos =>
      todos.get(request.path.id) match
        case Some(todo) => new GetTodoById.Response200(todo)
        case None       => new GetTodoById.Response404(TodoError(404, s"Todo ${request.path.id.value} not found"))
    }

  override def deleteTodoById(request: DeleteTodoById.Request): Task[DeleteTodoById.Response[?]] =
    store.modify { todos =>
      todos.get(request.path.id) match
        case Some(todo) => (new DeleteTodoById.Response200(todo), todos - request.path.id)
        case None       => (new DeleteTodoById.Response404(TodoError(404, s"Todo ${request.path.id.value} not found")), todos)
    }

  override def getUsers(request: GetUsers.Request.type): Task[GetUsers.Response[?]] =
    ZIO.succeed(new GetUsers.Response200(List(User("Willem"), User("Maureen"))))

object TodoService:
  val layer: ULayer[
    GetTodos.Handler[Task]
      & PostTodo.Handler[Task]
      & GetTodoById.Handler[Task]
      & DeleteTodoById.Handler[Task]
      & GetUsers.Handler[Task]
  ] =
    ZLayer.fromZIO(Ref.make(Map.empty[TodoId, TodoDto]).map(new TodoService(_)))
