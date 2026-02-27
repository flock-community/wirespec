package example

import community.flock.wirespec.generated.endpoint.GetTodos
import community.flock.wirespec.scala.Wirespec
import zio.*
import zio.http.*

object TodoRoutes:
  def routes(serialization: Wirespec.Serialization): Routes[GetTodos.Handler[Task], Throwable] =
    Routes(
      Method.GET / "api" / "todos" ->
        ZIOHttpFromWirespec.handle(serialization, GetTodos.Server) { req =>
          ZIO.serviceWithZIO[GetTodos.Handler[Task]](_.getTodos(req))
        }
    )
