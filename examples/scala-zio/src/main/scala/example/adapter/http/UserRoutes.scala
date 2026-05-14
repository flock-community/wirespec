package example.adapter.http

import community.flock.wirespec.generated.endpoint.GetUsers
import community.flock.wirespec.scala.Wirespec
import example.adapter.http.ZIOHttpFromWirespec.*
import example.port.UserService
import zio.*
import zio.http.*

object UserRoutes:
  def routes: Routes[Wirespec.Serialization & UserService, Response] =
    Routes(
      GetUsers.Server.toRoute { _ =>
        ZIO.serviceWithZIO[UserService](_.getUsers.map(new GetUsers.Response200(_)))
      },
    ).handleError(e => Response.internalServerError(e.getMessage))
