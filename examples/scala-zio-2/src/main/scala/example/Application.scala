package example

import example.adapter.http.{JsonSerialization, TodoRoutes, UserRoutes}
import example.adapter.store.{InMemoryTodoRepository, InMemoryUserRepository}
import example.service.{TodoService, UserService}
import zio.ZIOAppDefault
import zio.http.Server

object Application extends ZIOAppDefault:
  override def run =
    Server
      .serve(
        TodoRoutes.routes(JsonSerialization) ++
        UserRoutes.routes(JsonSerialization)
      )
      .provide(
        Server.defaultWithPort(8080),
        TodoService.layer,
        UserService.layer,
        InMemoryTodoRepository.layer,
        InMemoryUserRepository.layer,
      )
