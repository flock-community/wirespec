package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class CompileUnionTest {

    private val logger = noLogger

    private val compiler = compile(
        """
            |union UserAccount {UserAccountPassword, UserAccountToken}
            |type UserAccountPassword {
            |  username: String,
            |  password: String
            |}
            |type UserAccountToken {
            |  token: String
            |}
            |type User {
            |   username: String,
            |   account: UserAccount
            |}
        """.trimMargin()
    )

    @Test
    fun testUnionKotlin() {
        val kotlin = """
            |package community.flock.wirespec.generated
            |
            |sealed interface UserAccount
            |data class UserAccountPassword(
            |  val username: String,
            |  val password: String
            |): UserAccount
            |data class UserAccountToken(
            |  val token: String
            |): UserAccount
            |data class User(
            |  val username: String,
            |  val account: UserAccount
            |)
            |
        """.trimMargin()

        compiler(KotlinEmitter(logger = logger)) shouldBeRight kotlin
    }

    private fun compile(source: String) = { emitter: Emitter ->
        Wirespec.compile(source)(logger)(emitter)
            .map { it.first().result }
            .onLeft(::println)
    }
}
