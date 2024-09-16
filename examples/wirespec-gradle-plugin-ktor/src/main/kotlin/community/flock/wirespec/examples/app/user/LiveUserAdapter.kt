package community.flock.wirespec.examples.app.user

import community.flock.wirespec.examples.app.user.UserConverter.internalize
import community.flock.wirespec.generated.kotlin.DeleteUserByNameEndpoint
import community.flock.wirespec.generated.kotlin.GetUserByNameEndpoint
import community.flock.wirespec.generated.kotlin.GetUsersEndpoint
import community.flock.wirespec.generated.kotlin.PostUserEndpoint
import community.flock.wirespec.generated.kotlin.UserDto
import kotlinx.coroutines.runBlocking

class LiveUserAdapter(private val client: UserClient) : UserAdapter {

    override fun getAllUsers(): List<User> = runBlocking {
        when (val res = client.getUsers(GetUsersEndpoint.Request)) {
            is GetUsersEndpoint.Response200 -> res.body.map { it.internalize() }
        }
    }

    override fun getUserByName(name: String): User = runBlocking {
        when (val res = client.getUserByName(GetUserByNameEndpoint.Request(name))) {
            is GetUserByNameEndpoint.Response200 -> res.body.internalize()
            is GetUserByNameEndpoint.Response404 -> TODO()
        }
    }

    override fun saveUser(user: User): User = runBlocking {
        when (val res = client.postUser(PostUserEndpoint.Request(UserDto(name = user.name)))) {
            is PostUserEndpoint.Response200 -> res.body.internalize()
            is PostUserEndpoint.Response409 -> TODO()
        }
    }

    override fun deleteUserByName(name: String): User = runBlocking {
        when (val res = client.deleteUserByName(DeleteUserByNameEndpoint.Request(name))) {
            is DeleteUserByNameEndpoint.Response200 -> res.body.internalize()
            is DeleteUserByNameEndpoint.Response404 -> TODO()
        }
    }
}

