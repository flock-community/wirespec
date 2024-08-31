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

class LiveUserClient(private val client: HttpClient = HttpClient()) : UserClient {

    override suspend fun getUsers(request: GetUsersEndpoint.Request) =
        GetUsersEndpoint.externalizeRequest(Serialization, request)
            .let(::handle)
            .let { GetUsersEndpoint.internalizeResponse(Serialization, it) }

    override suspend fun getUserByName(request: GetUserByNameEndpoint.Request) =
        GetUserByNameEndpoint.externalizeRequest(Serialization, request)
            .let(::handle)
            .let { GetUserByNameEndpoint.internalizeResponse(Serialization, it) }

    override suspend fun postUser(request: PostUserEndpoint.Request) =
        PostUserEndpoint.externalizeRequest(Serialization, request)
            .let(::handle)
            .let { PostUserEndpoint.internalizeResponse(Serialization, it) }

    override suspend fun deleteUserByName(request: DeleteUserByNameEndpoint.Request) =
        DeleteUserByNameEndpoint.externalizeRequest(Serialization, request)
            .let(::handle)
            .let { DeleteUserByNameEndpoint.internalizeResponse(Serialization, it) }

    private fun handle(request: Wirespec.RawRequest): Wirespec.RawResponse = runBlocking {
        val response = client.request("http://localhost:8080/") {
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
