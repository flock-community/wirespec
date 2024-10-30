package community.flock.wirespec.example.maven.custom.app.user

import community.flock.wirespec.example.maven.custom.app.common.Serialization
import community.flock.wirespec.example.maven.custom.app.common.WirespecClient
import community.flock.wirespec.generated.kotlin.DeleteUserByNameEndpoint
import community.flock.wirespec.generated.kotlin.GetUserByNameEndpoint
import community.flock.wirespec.generated.kotlin.GetUsersEndpoint
import community.flock.wirespec.generated.kotlin.PostUserEndpoint

interface UserClient :
    GetUsersEndpoint.Handler,
    GetUserByNameEndpoint.Handler,
    PostUserEndpoint.Handler,
    DeleteUserByNameEndpoint.Handler

class LiveUserClient(private val wirespec: WirespecClient) : UserClient {

    override suspend fun getUsers(request: GetUsersEndpoint.Request) =
        with(GetUsersEndpoint.Handler.client(Serialization)) {
            to(request).let(wirespec::handle).let(::from)
        }

    override suspend fun getUserByName(request: GetUserByNameEndpoint.Request) =
        with(GetUserByNameEndpoint.Handler.client(Serialization)) {
            to(request).let(wirespec::handle).let(::from)
        }

    override suspend fun postUser(request: PostUserEndpoint.Request) =
        with(PostUserEndpoint.Handler.client(Serialization)) {
            to(request).let(wirespec::handle).let(::from)
        }

    override suspend fun deleteUserByName(request: DeleteUserByNameEndpoint.Request) =
        with(DeleteUserByNameEndpoint.Handler.client(Serialization)) {
            to(request).let(wirespec::handle).let(::from)
        }
}
