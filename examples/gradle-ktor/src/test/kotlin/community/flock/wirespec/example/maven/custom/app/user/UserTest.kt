package community.flock.wirespec.example.maven.custom.app.user

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

interface TestContext {
    val userService: UserService
}

class UserTest {
    @Test
    fun testGetAllUsers() = testContext {
        assertTrue { userService.getAllUsers().isNotEmpty() }
    }

    @Test
    fun testGetUserByName() = testContext {
        assertEquals(User("name"), userService.getUserByName("name"))
    }

    @Test
    fun testSaveUser() = testContext {
        assertEquals(
            User("newName"), userService.saveUser(
                User(
                    "newName"
                )
            ))
    }

    @Test
    fun testDeleteUserByName() = testContext {
        userService.saveUser(User("newName"))
        assertEquals(User("newName"), userService.deleteUserByName("newName"))
    }
}

private fun testContext(test: TestContext.() -> Unit) = object : TestContext {
    override val userService = object : UserService {
        override val userAdapter = LiveUserAdapter(TestUserClient())
    }
}.test()
