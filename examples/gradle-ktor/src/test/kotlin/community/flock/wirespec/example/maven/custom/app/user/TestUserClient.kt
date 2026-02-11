package community.flock.wirespec.example.maven.custom.app.user

import community.flock.wirespec.generated.kotlin.endpoint.DeleteUserByName
import community.flock.wirespec.generated.kotlin.endpoint.GetUserByName
import community.flock.wirespec.generated.kotlin.endpoint.GetUsers
import community.flock.wirespec.generated.kotlin.endpoint.PostUser
import community.flock.wirespec.generated.kotlin.model.UserDto

class TestUserClient : UserClient {
    private val users =
        mutableSetOf(
            UserDto("name"),
        )

    override suspend fun getUsers(request: GetUsers.Request) = users
        .toList()
        .let(GetUsers::Response200)

    override suspend fun getUserByName(request: GetUserByName.Request) = users
        .find { it.name == request.path.name }
        ?.let(GetUserByName::Response200)
        ?: GetUserByName.Response404

    override suspend fun postUser(request: PostUser.Request) = request.body
        .takeIf(users::add)
        ?.let(PostUser::Response200)
        ?: PostUser.Response409

    override suspend fun deleteUserByName(request: DeleteUserByName.Request) = users
        .find { it.name == request.path.name }
        ?.also(users::remove)
        ?.let(DeleteUserByName::Response200)
        ?: DeleteUserByName.Response404
}
