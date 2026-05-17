package example.service

import community.flock.wirespec.generated.model.User
import example.port.{UserRepository, UserService}
import zio.*

class UserServiceLive(repo: UserRepository) extends UserService:
  override def getUsers: Task[List[User]] =
    repo.findAll

object UserServiceLive:
  val layer: URLayer[UserRepository, UserService] =
    ZLayer.fromZIO(ZIO.serviceWith[UserRepository](new UserServiceLive(_)))
