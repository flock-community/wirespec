package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class CompileUnionTest {

    private val compiler = compile(
        """
        type UserAccount = UserAccountPassword | UserAccountToken
        type UserAccountPassword {
          username: String,
          password: String
        }
        type UserAccountToken {
          token: String
        }
        type User {
           username: String,
           account: UserAccount
        }
        """.trimIndent()
    )

    @Test
    fun kotlin() {
        val expected = """
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

        compiler(KotlinEmitter()) shouldBeRight expected
    }

    @Test
    fun java() {
        val java = """
            |package community.flock.wirespec.generated;
            |
            |public sealed interface UserAccount permits UserAccountPassword, UserAccountToken {}
            |
        """.trimMargin()

        compiler(JavaEmitter()) shouldBeRight java
    }

    @Test
    fun scala() {
        val scala = """
            |package community.flock.wirespec.generated
            |
            |// TODO("Not yet implemented")
            |
            |case class UserAccountPassword(
            |  val username: String,
            |  val password: String
            |)
            |
            |
            |case class UserAccountToken(
            |  val token: String
            |)
            |
            |
            |case class User(
            |  val username: String,
            |  val account: UserAccount
            |)
            |
            |
        """.trimMargin()

        compiler(ScalaEmitter()) shouldBeRight scala
    }

    @Test
    fun typeScript() {
        val ts = """
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

        compiler(TypeScriptEmitter()) shouldBeRight ts
    }

    @Test
    fun wireSpec() {
        val wirespec = """
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

        compiler(WirespecEmitter()) shouldBeRight wirespec
    }
}
