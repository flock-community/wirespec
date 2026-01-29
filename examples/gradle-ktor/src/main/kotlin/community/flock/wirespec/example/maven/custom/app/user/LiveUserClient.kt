package community.flock.wirespec.example.maven.custom.app.user

import community.flock.wirespec.example.maven.custom.app.common.Serialization
import community.flock.wirespec.example.maven.custom.app.common.WirespecClient
import community.flock.wirespec.generated.kotlin.endpoint.DeleteUserByName
import community.flock.wirespec.generated.kotlin.endpoint.GetUserByName
import community.flock.wirespec.generated.kotlin.endpoint.GetUsers
import community.flock.wirespec.generated.kotlin.endpoint.PostUser

interface UserClient :
    GetUsers.Handler,
    GetUserByName.Handler,
    PostUser.Handler,
    DeleteUserByName.Handler

class LiveUserClient(
    private val wirespec: WirespecClient,
) : UserClient {
    override suspend fun getUsers(request: GetUsers.Request) = with(GetUsers.Adapter) {
        request
            .let { toRawRequest(Serialization, it) }
            .let(wirespec::transport)
            .let { fromRawResponse(Serialization, it) }
    }

    override suspend fun getUserByName(request: GetUserByName.Request) = with(GetUserByName.Adapter) {
        request
            .let { toRawRequest(Serialization, it) }
            .let(wirespec::transport)
            .let { fromRawResponse(Serialization, it) }
    }

    override suspend fun postUser(request: PostUser.Request) = with(PostUser.Adapter) {
        request
            .let { toRawRequest(Serialization, it) }
            .let(wirespec::transport)
            .let { fromRawResponse(Serialization, it) }
    }

    override suspend fun deleteUserByName(request: DeleteUserByName.Request) = with(DeleteUserByName.Adapter) {
        request
            .let { toRawRequest(Serialization, it) }
            .let(wirespec::transport)
            .let { fromRawResponse(Serialization, it) }
    }
}
