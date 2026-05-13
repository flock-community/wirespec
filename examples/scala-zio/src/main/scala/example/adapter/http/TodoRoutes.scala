package example.adapter.http

import community.flock.wirespec.generated.endpoint.{DeleteTodoById, GetTodoById, GetTodos, PostTodo}
import community.flock.wirespec.generated.model.TodoError
import community.flock.wirespec.scala.Wirespec
import example.adapter.http.ZIOHttpFromWirespec.*
import example.port.TodoService
import zio.*
import zio.http.*

object TodoRoutes:
  def routes: Routes[Wirespec.Serialization & TodoService, Response] =
    Routes(
      GetTodos.Server.toRoute { req =>
        ZIO.serviceWithZIO[TodoService](
          _.getTodos(req.queries.done).map(todos =>
            new GetTodos.Response200(xTotal = todos.size.toLong, body = todos)
          )
        )
      },
      PostTodo.Server.toRoute { req =>
        ZIO.serviceWithZIO[TodoService](
          _.createTodo(req.body).map(new PostTodo.Response200(_))
        )
      },
      GetTodoById.Server.toRoute { req =>
        ZIO.serviceWithZIO[TodoService](
          _.getTodoById(req.path.id).map {
            case Some(todo) => new GetTodoById.Response200(todo)
            case None       => new GetTodoById.Response404(TodoError(404, s"Todo ${req.path.id.value} not found"))
          }
        )
      },
      DeleteTodoById.Server.toRoute { req =>
        ZIO.serviceWithZIO[TodoService](
          _.deleteTodoById(req.path.id).map {
            case Some(todo) => new DeleteTodoById.Response200(todo)
            case None       => new DeleteTodoById.Response404(TodoError(404, s"Todo ${req.path.id.value} not found"))
          }
        )
      },
    ).handleError(e => Response.internalServerError(e.getMessage))
