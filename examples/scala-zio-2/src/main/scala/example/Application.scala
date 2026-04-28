package example

import community.flock.wirespec.scala.Wirespec
import example.adapter.http.{JsonSerialization, TodoRoutes, UserRoutes}
import example.adapter.store.{InMemoryTodoRepository, InMemoryUserRepository}
import example.service.{TodoServiceLive, UserService}
import zio.ZIOAppDefault
import zio.http.Server
import zio.ZLayer

object Application extends ZIOAppDefault:
  override def run =
    Server
      .serve(TodoRoutes.routes ++ UserRoutes.routes)
      .provide(
        Server.defaultWithPort(8080),
        ZLayer.succeed[Wirespec.Serialization](JsonSerialization),
        TodoServiceLive.layer,
        UserService.layer,
        InMemoryTodoRepository.layer,
        InMemoryUserRepository.layer,
      )
