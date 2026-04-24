package example

import community.flock.wirespec.generated.endpoint.GetUsers
import community.flock.wirespec.scala.Wirespec
import example.ZIOHttpFromWirespec.*
import zio.*
import zio.http.*

object UserRoutes:
  def routes(serialization: Wirespec.Serialization): Routes[
    GetUsers.Handler[Task],
    Throwable
  ] =
    Routes(
      GetUsers.Server.toRoute(serialization) { req =>
        ZIO.serviceWithZIO[GetUsers.Handler[Task]](_.getUsers(req))
      },
    )
