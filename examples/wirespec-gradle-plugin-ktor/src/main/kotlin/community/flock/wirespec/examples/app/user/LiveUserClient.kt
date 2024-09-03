package community.flock.wirespec.examples.app.user

import community.flock.wirespec.Wirespec
import community.flock.wirespec.examples.app.common.Serialization
import community.flock.wirespec.generated.kotlin.DeleteUserByNameEndpoint
import community.flock.wirespec.generated.kotlin.GetUserByNameEndpoint
import community.flock.wirespec.generated.kotlin.GetUsersEndpoint
import community.flock.wirespec.generated.kotlin.PostUserEndpoint
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.headers
import io.ktor.http.path
import kotlinx.coroutines.runBlocking

interface UserClient :
    GetUsersEndpoint.Handler,
    GetUserByNameEndpoint.Handler,
    PostUserEndpoint.Handler,
    DeleteUserByNameEndpoint.Handler

class LiveUserClient(private val httpClient: HttpClient = HttpClient()) : UserClient {

    override suspend fun getUsers(request: GetUsersEndpoint.Request) =
        with(GetUsersEndpoint.Handler.client(Serialization)) {
            externalize(request).let(::handle).let(::internalize)
        }

    override suspend fun getUserByName(request: GetUserByNameEndpoint.Request) =
        with(GetUserByNameEndpoint.Handler.client(Serialization)) {
            externalize(request).let(::handle).let(::internalize)
        }

    override suspend fun postUser(request: PostUserEndpoint.Request) =
        with(PostUserEndpoint.Handler.client(Serialization)) {
            externalize(request).let(::handle).let(::internalize)
        }

    override suspend fun deleteUserByName(request: DeleteUserByNameEndpoint.Request) =
        with(DeleteUserByNameEndpoint.Handler.client(Serialization)) {
            externalize(request).let(::handle).let(::internalize)
        }

    private fun handle(request: Wirespec.RawRequest): Wirespec.RawResponse = runBlocking {
        val response = httpClient.request("http://localhost:8080/") {
            method = HttpMethod.parse(request.method)
            url {
                path(*request.path.toTypedArray())
            }
            headers {
                request.headers.forEach { (t, u) -> appendAll(t, u) }
            }
        }
        response.run {
            Wirespec.RawResponse(
                statusCode = status.value,
                headers = headers.entries().associate { it.key to it.value },
                body = body()
            )
        }
    }
}
