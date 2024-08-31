package community.flock.wirespec.examples.app.user

interface UserService : HasUserAdapter

fun UserService.getAllUsers() = userAdapter.getAllUsers()

fun UserService.getUserByName(name: String) = userAdapter.getUserByName(name)

fun UserService.saveUser(user: User) = userAdapter.saveUser(user)

fun UserService.deleteUserByName(name: String) = userAdapter.deleteUserByName(name)
