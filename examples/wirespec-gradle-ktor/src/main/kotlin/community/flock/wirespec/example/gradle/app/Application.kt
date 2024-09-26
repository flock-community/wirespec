package community.flock.wirespec.example.gradle.app

import community.flock.wirespec.example.gradle.app.todo.LiveTodoRepository
import community.flock.wirespec.example.gradle.app.todo.todoModule
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
    todoModule(LiveTodoRepository())
}
