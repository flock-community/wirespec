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

    override suspend fun getUsers() = users
        .toList()
        .let(GetUsers::Response200)

    override suspend fun getUserByName(name: String) = users
        .find { it.name == name }
        ?.let(GetUserByName::Response200)
        ?: GetUserByName.Response404(Unit)

    override suspend fun postUser(body: UserDto) = body
        .takeIf(users::add)
        ?.let(PostUser::Response200)
        ?: PostUser.Response409(Unit)

    override suspend fun deleteUserByName(name: String) = users
        .find { it.name == name }
        ?.also(users::remove)
        ?.let(DeleteUserByName::Response200)
        ?: DeleteUserByName.Response404(Unit)
}
