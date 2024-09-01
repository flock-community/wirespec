package community.flock.wirespec.examples.app.user

import org.junit.Test
import kotlin.test.assertTrue

interface TestContext {
    val userService: UserService
}

class UserTest {
    @Test
    fun testUser() = testContext {
        assertTrue { userService.getAllUsers().isNotEmpty() }
    }
}

private fun testContext(test: TestContext.() -> Unit) = object : TestContext {
    override val userService = object : UserService {
        override val userAdapter = LiveUserAdapter(TestUserClient())
    }
}.test()
