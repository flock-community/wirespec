package example.adapter.store

import community.flock.wirespec.generated.model.User
import example.port.UserRepository
import zio.*

class InMemoryUserRepository extends UserRepository:
  override def findAll: Task[List[User]] =
    ZIO.succeed(List(User("Willem"), User("Maureen")))

object InMemoryUserRepository:
  val layer: ULayer[UserRepository] =
    ZLayer.succeed(new InMemoryUserRepository())
