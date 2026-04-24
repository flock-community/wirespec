package example.adapter.http

import community.flock.wirespec.generated.endpoint.{DeleteTodoById, GetTodoById, GetTodos, PostTodo}
import community.flock.wirespec.scala.Wirespec
import example.adapter.http.ZIOHttpFromWirespec.*
import zio.*
import zio.http.*

object TodoRoutes:
  def routes: Routes[
    Wirespec.Serialization
      & GetTodos.Handler[Task]
      & PostTodo.Handler[Task]
      & GetTodoById.Handler[Task]
      & DeleteTodoById.Handler[Task],
    Response
  ] =
    Routes(
      GetTodos.Server.toRoute { req =>
        ZIO.serviceWithZIO[GetTodos.Handler[Task]](_.getTodos(req))
      },
      PostTodo.Server.toRoute { req =>
        ZIO.serviceWithZIO[PostTodo.Handler[Task]](_.postTodo(req))
      },
      GetTodoById.Server.toRoute { req =>
        ZIO.serviceWithZIO[GetTodoById.Handler[Task]](_.getTodoById(req))
      },
      DeleteTodoById.Server.toRoute { req =>
        ZIO.serviceWithZIO[DeleteTodoById.Handler[Task]](_.deleteTodoById(req))
      },
    ).handleError(e => Response.internalServerError(e.getMessage))
