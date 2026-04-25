package example.port

import community.flock.wirespec.generated.model.User
import zio.*

trait UserService:
  def getUsers(): Task[List[User]]
