package example.adapter.http

import community.flock.wirespec.generated.endpoint.{DeleteTodoById, GetTodoById, GetTodos, PostTodo}
import community.flock.wirespec.scala.Wirespec
import example.adapter.http.ZIOHttpFromWirespec.*
import zio.*
import zio.http.*

object TodoRoutes:
  def routes(serialization: Wirespec.Serialization): Routes[
    GetTodos.Handler[Task]
      & PostTodo.Handler[Task]
      & GetTodoById.Handler[Task]
      & DeleteTodoById.Handler[Task],
    Response
  ] =
    Routes(
      GetTodos.Server.toRoute(serialization) { req =>
        ZIO.serviceWithZIO[GetTodos.Handler[Task]](_.getTodos(req))
      },
      PostTodo.Server.toRoute(serialization) { req =>
        ZIO.serviceWithZIO[PostTodo.Handler[Task]](_.postTodo(req))
      },
      GetTodoById.Server.toRoute(serialization) { req =>
        ZIO.serviceWithZIO[GetTodoById.Handler[Task]](_.getTodoById(req))
      },
      DeleteTodoById.Server.toRoute(serialization) { req =>
        ZIO.serviceWithZIO[DeleteTodoById.Handler[Task]](_.deleteTodoById(req))
      },
    ).handleError(e => Response.internalServerError(e.getMessage))
