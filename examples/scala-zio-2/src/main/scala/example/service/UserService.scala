package example.service

import community.flock.wirespec.generated.endpoint.GetUsers
import example.port.UserRepository
import zio.*

class UserService(repo: UserRepository) extends GetUsers.Handler[Task]:
  override def getUsers(request: GetUsers.Request.type): Task[GetUsers.Response[?]] =
    repo.findAll().map(new GetUsers.Response200(_))

object UserService:
  val layer: URLayer[UserRepository, GetUsers.Handler[Task]] =
    ZLayer.fromZIO(ZIO.service[UserRepository].map(new UserService(_)))
