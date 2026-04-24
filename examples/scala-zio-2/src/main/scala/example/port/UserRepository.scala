package example.port

import community.flock.wirespec.generated.model.User
import zio.*

trait UserRepository:
  def findAll(): Task[List[User]]
