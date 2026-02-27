object Application extends ZIOAppDefault:
  def run = Server
    .serve(
      GreetingRoutes() ++ DownloadRoutes() ++ CounterRoutes() ++ UserRoutes()
    )