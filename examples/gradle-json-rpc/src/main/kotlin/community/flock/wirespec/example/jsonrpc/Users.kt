package community.flock.wirespec.example.jsonrpc

import community.flock.wirespec.generated.model.User
import community.flock.wirespec.generated.model.UserNotFound
import community.flock.wirespec.generated.rpc.CreateUser
import community.flock.wirespec.generated.rpc.FindUsers
import community.flock.wirespec.generated.rpc.GetUser
import community.flock.wirespec.generated.rpc.RenameUser
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Keeps the users in memory, and implements the `Service` of every rpc, so the JSON-RPC server can call it. */
class Users :
    GetUser.Service,
    FindUsers.Service,
    CreateUser.Service,
    RenameUser.Service {

    private val users = ConcurrentHashMap<String, User>()

    private val ids = AtomicInteger()

    override suspend fun getUser(id: String): GetUser.Response = users[id]
        ?.let { GetUser.Result(value = it) }
        ?: GetUser.Error(value = UserNotFound(id = id))

    override suspend fun findUsers(name: String?): List<User> = users.values
        .filter { name == null || it.name.contains(name, ignoreCase = true) }
        .sortedBy { it.id.toInt() }

    override suspend fun createUser(name: String, email: String?): User = User(id = ids.incrementAndGet().toString(), name = name, email = email)
        .also { users[it.id] = it }

    override suspend fun renameUser(id: String, name: String): RenameUser.Response = users
        .computeIfPresent(id) { _, user -> user.copy(name = name) }
        ?.let { RenameUser.Result(value = it) }
        ?: RenameUser.Error(value = UserNotFound(id = id))
}
