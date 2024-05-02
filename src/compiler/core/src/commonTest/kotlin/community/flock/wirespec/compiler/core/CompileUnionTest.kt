package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CompileUnionTest {

    private val logger = noLogger

    private val compiler = compile(
        """
            |type UserAccount = UserAccountPassword | UserAccountToken
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
        val expected =
            """
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

        compiler(KotlinEmitter(logger = logger))
            .shouldBeRight()
            .apply {
                first().second shouldBe expected
            }
    }

    @Test
    fun testUnionJava() {

        compiler(JavaEmitter(logger = logger))
            .shouldBeRight()
            .apply {
                val (account, password, token, user) = this

                account.first shouldBe "UserAccount"
                account.second shouldBe """
                    |package community.flock.wirespec.generated;
                    |
                    |sealed interface UserAccount permits UserAccountPassword, UserAccountToken {}
                    |
                """.trimMargin()

                password.first shouldBe "UserAccountPassword"
                password.second shouldBe """
                    |package community.flock.wirespec.generated;
                    |
                    |public record UserAccountPassword (
                    |  String username,
                    |  String password
                    |) implements UserAccount {
                    |};
                    |
                """.trimMargin()

                token.first shouldBe "UserAccountToken"
                token.second shouldBe """
                    |package community.flock.wirespec.generated;
                    |
                    |public record UserAccountToken (
                    |  String token
                    |) implements UserAccount {
                    |};
                    |
                """.trimMargin()

                user.first shouldBe "User"
                user.second shouldBe """
                    |package community.flock.wirespec.generated;
                    |
                    |public record User (
                    |  String username,
                    |  UserAccount account
                    |) {
                    |};
                    |
                """.trimMargin()
            }
    }

    @Test
    fun testUnionTypeScript() {

        compiler(TypeScriptEmitter(logger = logger))
            .shouldBeRight()
            .apply {
                first().second shouldBe """
                    |export type UserAccount = UserAccountPassword | UserAccountToken
                    |
                    |export type UserAccountPassword = {
                    |  "username": string,
                    |  "password": string
                    |}
                    |
                    |
                    |export type UserAccountToken = {
                    |  "token": string
                    |}
                    |
                    |
                    |export type User = {
                    |  "username": string,
                    |  "account": UserAccount
                    |}
                    |
                    |
                """.trimMargin()
            }
    }

    @Test
    fun testUnionWireSpec() {

        compiler(WirespecEmitter(logger = logger))
            .shouldBeRight()
            .apply {
                first().second shouldBe """
                    |type UserAccount = UserAccountPassword | UserAccountToken
                    |
                    |type UserAccountPassword {
                    |  username: String,
                    |  password: String
                    |}
                    |
                    |type UserAccountToken {
                    |  token: String
                    |}
                    |
                    |type User {
                    |  username: String,
                    |  account: UserAccount
                    |}
                    |
                """.trimMargin()
            }
    }

    private fun compile(source: String) = { emitter: Emitter ->
        WirespecSpec.compile(source)(logger)(emitter)
            .map { emittedList -> emittedList.map { it.typeName to it.result } }
            .onLeft(::println)
    }
}
