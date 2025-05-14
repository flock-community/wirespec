package community.flock.wirespec.example.maven.custom.app.user

import community.flock.wirespec.example.maven.custom.app.user.UserConverter.internalize
import community.flock.wirespec.generated.kotlin.endpoint.DeleteUserByName
import community.flock.wirespec.generated.kotlin.endpoint.GetUserByName
import community.flock.wirespec.generated.kotlin.endpoint.GetUsers
import community.flock.wirespec.generated.kotlin.endpoint.PostUser
import community.flock.wirespec.generated.kotlin.model.UserDto
import kotlinx.coroutines.runBlocking

class LiveUserAdapter(
    private val client: UserClient,
) : UserAdapter {
    override fun getAllUsers(): List<User> = runBlocking {
        when (val res = client.getUsers(GetUsers.Request)) {
            is GetUsers.Response200 -> res.body.map { it.internalize() }
        }
    }

    override fun getUserByName(name: String): User = runBlocking {
        when (val res = client.getUserByName(GetUserByName.Request(name))) {
            is GetUserByName.Response200 -> res.body.internalize()
            is GetUserByName.Response404 -> TODO()
        }
    }

    override fun saveUser(user: User): User = runBlocking {
        when (val res = client.postUser(PostUser.Request(UserDto(name = user.name)))) {
            is PostUser.Response200 -> res.body.internalize()
            is PostUser.Response409 -> TODO()
        }
    }

    override fun deleteUserByName(name: String): User = runBlocking {
        when (val res = client.deleteUserByName(DeleteUserByName.Request(name))) {
            is DeleteUserByName.Response200 -> res.body.internalize()
            is DeleteUserByName.Response404 -> TODO()
        }
    }
}
