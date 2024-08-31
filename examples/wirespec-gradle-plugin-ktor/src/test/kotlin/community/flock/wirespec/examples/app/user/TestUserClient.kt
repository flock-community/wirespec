package community.flock.wirespec.examples.app.user

import community.flock.wirespec.generated.kotlin.DeleteUserByNameEndpoint
import community.flock.wirespec.generated.kotlin.GetUserByNameEndpoint
import community.flock.wirespec.generated.kotlin.GetUsersEndpoint
import community.flock.wirespec.generated.kotlin.PostUserEndpoint
import community.flock.wirespec.generated.kotlin.UserDto

class TestUserClient : UserClient {

    private val users = mutableListOf(
        UserDto("name")
    )

    override suspend fun getUsers(request: GetUsersEndpoint.Request) =
        GetUsersEndpoint.Response200(body = users)

    override suspend fun getUserByName(request: GetUserByNameEndpoint.Request) =
        GetUserByNameEndpoint.Response200(body = users.first())

    override suspend fun postUser(request: PostUserEndpoint.Request) =
        PostUserEndpoint.Response200(body = users.first())

    override suspend fun deleteUserByName(request: DeleteUserByNameEndpoint.Request) =
        DeleteUserByNameEndpoint.Response200(body = users.first())
}
