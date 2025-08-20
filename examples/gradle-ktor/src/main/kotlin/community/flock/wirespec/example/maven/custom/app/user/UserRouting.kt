package community.flock.wirespec.example.maven.custom.app.user

import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.userModule(handler: UserHandler) {
    routing {
        val basePath = "/api/users"
        val namePathParam = "name"
        route(basePath, Get) {
            handle {
                call.respond(handler.getAllUsers())
            }
        }
        route("$basePath/{$namePathParam}", Get) {
            handle {
                val name = call.parameters["name"] ?: error("Invalid request")
                call.respond(handler.getUserByName(name))
            }
        }
        route(basePath, Post) {
            handle {
                call.respond(handler.getAllUsers())
            }
        }
        route("$basePath/{$namePathParam}", Delete) {
            handle {
                val name = call.parameters["name"] ?: error("Invalid request")
                call.respond(handler.deleteUserByName(name))
            }
        }
    }
}
