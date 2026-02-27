package example

import zio.ZIOAppDefault
import zio.http.{Routes, Server}

object Application extends ZIOAppDefault:
  def run = Server
    .serve(Routes.empty)
    .provide(
      Server.defaultWithPort(8080),
    )