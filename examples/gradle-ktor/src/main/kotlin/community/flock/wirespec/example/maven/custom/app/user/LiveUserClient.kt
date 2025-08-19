package community.flock.wirespec.example.maven.custom.app.user

import community.flock.wirespec.generated.kotlin.Client
import community.flock.wirespec.generated.kotlin.client.DeleteUserByNameClient
import community.flock.wirespec.generated.kotlin.client.GetUserByNameClient
import community.flock.wirespec.generated.kotlin.client.GetUsersClient
import community.flock.wirespec.generated.kotlin.client.PostUserClient

interface UserClient :
    GetUsersClient,
    GetUserByNameClient,
    PostUserClient,
    DeleteUserByNameClient

class LiveUserClient(wirespec: Client) :
    UserClient,
    GetUsersClient by wirespec,
    GetUserByNameClient by wirespec,
    PostUserClient by wirespec,
    DeleteUserByNameClient by wirespec
