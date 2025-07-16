package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class WirespecEmitterTest {

    @Test
    fun compileFullEndpointTest() {
        val wirespec = """
            |endpoint PutTodo PUT PotentialTodoDto /todos/{id: String} ? {done: Boolean,name: String?} -> {
            |  200 -> TodoDto
            |  201 -> TodoDto
            |  500 -> Error
            |}
            |
            |type PotentialTodoDto {
            |  name: String,
            |  done: Boolean
            |}
            |
            |type Token {
            |  iss: String
            |}
            |
            |type TodoDto {
            |  id: String,
            |  name: String,
            |  done: Boolean
            |}
            |
            |type Error {
            |  code: Integer,
            |  description: String
            |}
            |
        """.trimMargin()

        CompileFullEndpointTest.compiler { WirespecEmitter() } shouldBeRight wirespec
    }

    @Test
    fun compileChannelTest() {
        val wirespec = """
            |channel Queue -> String
        """.trimMargin()

        CompileChannelTest.compiler { WirespecEmitter() } shouldBeRight wirespec
    }

    @Test
    fun compileEnumTest() {
        val wirespec = """
            |enum MyAwesomeEnum {
            |  ONE, Two, THREE_MORE, UnitedKingdom
            |}
            |
        """.trimMargin()

        CompileEnumTest.compiler { WirespecEmitter() } shouldBeRight wirespec
    }

    @Test
    fun compileMinimalEndpointTest() {
        val wirespec = """
            |endpoint GetTodos GET /todos -> {
            |  200 -> TodoDto[]
            |}
            |
            |type TodoDto {
            |  description: String
            |}
            |
        """.trimMargin()

        CompileMinimalEndpointTest.compiler { WirespecEmitter() } shouldBeRight wirespec
    }

    @Test
    fun compileRefinedTest() {
        val wirespec = """
            |type TodoId = String(/^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$/g)
            |
        """.trimMargin()

        CompileRefinedTest.compiler { WirespecEmitter() } shouldBeRight wirespec
    }

    @Test
    fun compileUnionTest() {
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

        CompileUnionTest.compiler { WirespecEmitter() } shouldBeRight wirespec
    }

    @Test
    fun compileTypeTest() {
        val wirespec = """
            |type Request {
            |  `type`: String,
            |  url: String,
            |  `BODY_TYPE`: String?,
            |  params: String[],
            |  headers: { String },
            |  body: { String?[]? }?
            |}
            |
        """.trimMargin()

        CompileTypeTest.compiler { WirespecEmitter() } shouldBeRight wirespec
    }
}
