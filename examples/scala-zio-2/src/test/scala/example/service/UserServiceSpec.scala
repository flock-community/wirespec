package example.service

import example.adapter.store.InMemoryUserRepository
import example.port.UserService
import zio.*
import zio.test.*

object UserServiceSpec extends ZIOSpecDefault:
  def spec = suite("UserServiceLive")(
    test("getUsers returns the list of users") {
      for
        svc   <- ZIO.service[UserService]
        users <- svc.getUsers()
      yield assertTrue(users.nonEmpty)
    },
  ).provide(UserServiceLive.layer, InMemoryUserRepository.layer)
