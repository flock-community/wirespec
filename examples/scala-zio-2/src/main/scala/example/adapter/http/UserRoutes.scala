package example.adapter.http

import community.flock.wirespec.generated.endpoint.GetUsers
import community.flock.wirespec.scala.Wirespec
import example.adapter.http.ZIOHttpFromWirespec.*
import zio.*
import zio.http.*

object UserRoutes:
  def routes: Routes[
    Wirespec.Serialization & GetUsers.Handler[Task],
    Response
  ] =
    Routes(
      GetUsers.Server.toRoute { req =>
        ZIO.serviceWithZIO[GetUsers.Handler[Task]](_.getUsers(req))
      },
    ).handleError(e => Response.internalServerError(e.getMessage))
