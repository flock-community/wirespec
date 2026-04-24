package example

import zio.ZIOAppDefault
import zio.http.Server

object Application extends ZIOAppDefault:
  override def run =
    Server
      .serve(TodoRoutes.routes(JsonSerialization) ++ UserRoutes.routes(JsonSerialization))
      .provide(
        Server.defaultWithPort(8080),
        TodoService.layer,
      )
