package example

import community.flock.wirespec.generated.endpoint.GetTodos
import example.ZIOHttpFromWirespec.*
import zio.*
import zio.http.*
import zio.http.codec.PathCodec

object TodoRoutes:
  def routes(): Routes[GetTodos.Handler[List], Nothing] =
    Routes(
      GetTodos.Server.zioPath -> handler {
          ZIO.serviceWith[GetTodos.Handler[List]](_.getTodos).map()
      },
//      Method.POST / "todos" -> handler {
//        ZIO.succeed(Response.text("Create a new todo"))
//      },
//      Method.GET / "todos" / path[Int] -> handler { id =>
//        ZIO.succeed(Response.text(s"Get todo with id: $id"))
//      },
//      Method.PUT / "todos" / path[Int] -> handler { id =>
//        ZIO.succeed(Response.text(s"Update todo with id: $id"))
//      },
//      Method.DELETE / "todos" / path[Int] -> handler { id =>
//        ZIO.succeed(Response.text(s"Delete todo with id: $id"))
//      }
    )
