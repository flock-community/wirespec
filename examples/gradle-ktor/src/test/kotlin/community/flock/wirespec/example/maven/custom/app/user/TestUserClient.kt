package community.flock.wirespec.example.maven.custom.app.user

import community.flock.wirespec.generated.kotlin.DeleteUserByNameEndpoint
import community.flock.wirespec.generated.kotlin.GetUserByNameEndpoint
import community.flock.wirespec.generated.kotlin.GetUsersEndpoint
import community.flock.wirespec.generated.kotlin.PostUserEndpoint
import community.flock.wirespec.generated.kotlin.UserDto

class TestUserClient : UserClient {

    private val users = mutableSetOf(
        UserDto("name")
    )

    override suspend fun getUsers(request: GetUsersEndpoint.Request) = users
        .toList()
        .let(GetUsersEndpoint::Response200)

    override suspend fun getUserByName(request: GetUserByNameEndpoint.Request) = users
        .find { it.name == request.path.name }
        ?.let(GetUserByNameEndpoint::Response200)
        ?: GetUserByNameEndpoint.Response404(Unit)

    override suspend fun postUser(request: PostUserEndpoint.Request) = request.body
        .takeIf(users::add)
        ?.let(PostUserEndpoint::Response200)
        ?: PostUserEndpoint.Response409(Unit)

    override suspend fun deleteUserByName(request: DeleteUserByNameEndpoint.Request) = users
        .find { it.name == request.path.name }
        ?.also(users::remove)
        ?.let(DeleteUserByNameEndpoint::Response200)
        ?: DeleteUserByNameEndpoint.Response404(Unit)
}
