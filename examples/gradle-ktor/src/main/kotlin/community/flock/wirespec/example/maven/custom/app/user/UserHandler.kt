package community.flock.wirespec.example.maven.custom.app.user

class UserHandler(private val service: UserService) {
    fun getAllUsers() = service.getAllUsers()
    fun getUserByName(name: String) = service.getUserByName(name)
    fun putOneUser(user: User) = service.saveUser(user)
    fun deleteUserByName(name: String) = service.deleteUserByName(name)
}
