package community.flock.wirespec.example.maven.custom.app

import community.flock.wirespec.example.maven.custom.app.common.Serialization
import community.flock.wirespec.example.maven.custom.app.common.WirespecClient
import community.flock.wirespec.example.maven.custom.app.todo.LiveTodoRepository
import community.flock.wirespec.example.maven.custom.app.todo.TodoHandler
import community.flock.wirespec.example.maven.custom.app.todo.TodoService
import community.flock.wirespec.example.maven.custom.app.todo.todoModule
import community.flock.wirespec.example.maven.custom.app.user.LiveUserAdapter
import community.flock.wirespec.example.maven.custom.app.user.UserHandler
import community.flock.wirespec.example.maven.custom.app.user.UserService
import community.flock.wirespec.example.maven.custom.app.user.userModule
import community.flock.wirespec.generated.kotlin.Client
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        config()
        app()
    }.start(wait = true)
}

fun Application.config() {
    install(ContentNegotiation) {
        json()
    }
}

fun Application.app() {
    object : TodoService {
        override val todoRepository = LiveTodoRepository()
    }
        .let(::TodoHandler)
        .let(::todoModule)

    object : UserService {
        override val userAdapter = LiveUserAdapter(
            client = Client(
                serialization = Serialization,
                handler = WirespecClient::handle,
            ),
        )
    }
        .let(::UserHandler)
        .let(::userModule)
}
