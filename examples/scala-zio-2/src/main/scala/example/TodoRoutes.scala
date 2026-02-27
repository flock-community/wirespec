package example

import community.flock.wirespec.generated.endpoint.{GetTodoById, GetTodos, PostTodo}
import community.flock.wirespec.scala.Wirespec
import example.ZIOHttpFromWirespec.*
import zio.*
import zio.http.*

object TodoRoutes:
  def routes(serialization: Wirespec.Serialization): Routes[
    GetTodos.Handler[Task] & PostTodo.Handler[Task] & GetTodoById.Handler[Task],
    Throwable
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
    )
