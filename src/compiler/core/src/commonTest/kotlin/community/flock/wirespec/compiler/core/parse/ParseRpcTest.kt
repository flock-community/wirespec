package community.flock.wirespec.compiler.core.parse

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseRpcTest {

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source))).map { it.modules.flatMap(Module::statements) }

    @Test
    fun testRpcParser() {
        val source =
            // language=ws
            """
            |type Todo { name: String }
            |type TodoError { code: String }
            |rpc GetTodo {
            |  id: String,
            |  verbose: Boolean
            |} -> Todo ! TodoError
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(3)[2]
            .shouldBeInstanceOf<Rpc>()
            .run {
                comment?.value shouldBe null
                identifier.value shouldBe "GetTodo"
                shape.value.shouldHaveSize(2)
                shape.value[0].identifier.value shouldBe "id"
                shape.value[1].identifier.value shouldBe "verbose"
                result.shouldBeInstanceOf<Reference.Custom>().value shouldBe "Todo"
                error.shouldBeInstanceOf<Reference.Custom>().value shouldBe "TodoError"
            }
    }

    @Test
    fun testRpcParserWithoutError() {
        val source =
            // language=ws
            """
            |type Todo { name: String }
            |rpc GetTodo {
            |  id: String
            |} -> Todo
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Rpc>()
            .run {
                identifier.value shouldBe "GetTodo"
                shape.value.shouldHaveSize(1)
                result.shouldBeInstanceOf<Reference.Custom>().value shouldBe "Todo"
                error shouldBe null
            }
    }

    @Test
    fun testRpcParserEmptyShape() {
        val source =
            // language=ws
            """
            |rpc Ping {} -> String
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(1)[0]
            .shouldBeInstanceOf<Rpc>()
            .run {
                identifier.value shouldBe "Ping"
                shape.value.shouldHaveSize(0)
                result.shouldBeInstanceOf<Reference.Primitive>()
                error shouldBe null
            }
    }

    @Test
    fun testRpcParserWithoutBodyFails() {
        val source =
            // language=ws
            """
            |rpc GetTodo -> String
            """.trimMargin()

        parser(source).shouldBeLeft()
    }
}
