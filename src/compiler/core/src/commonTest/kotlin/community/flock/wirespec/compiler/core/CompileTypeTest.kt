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

class CompileTypeTest {

    private val logger = noLogger

    private val compiler = compile(
        """
            |type User {
            |  `type`: String,
            |  username: String,
            |  password: String
            |}
        """.trimMargin()
    )

    @Test
    fun testTypeKotlin() {
        val expected =
            """
                |package community.flock.wirespec.generated
                |
                |data class User(
                |  val type: String,
                |  val username: String,
                |  val password: String
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
    fun testTypeJava() {

        compiler(JavaEmitter(logger = logger))
            .shouldBeRight()
            .apply {
                val (user) = this

                user.first shouldBe "User"
                user.second shouldBe """
                    |package community.flock.wirespec.generated;
                    |
                    |public record User(
                    |  String type,
                    |  String username,
                    |  String password
                    |){
                    |};
                    |
                """.trimMargin()
            }
    }

    @Test
    fun testTypeTypeScript() {

        compiler(TypeScriptEmitter(logger = logger))
            .shouldBeRight()
            .apply {
                first().second shouldBe """
                    |export type User = {
                    |  "type": string,
                    |  "username": string,
                    |  "password": string
                    |}
                    |
                    |
                """.trimMargin()
            }
    }

    @Test
    fun testTypeWireSpec() {

        compiler(WirespecEmitter(logger = logger))
            .shouldBeRight()
            .apply {
                first().second shouldBe """
                    |type User {
                    |  `type`: String,
                    |  username: String,
                    |  password: String
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
