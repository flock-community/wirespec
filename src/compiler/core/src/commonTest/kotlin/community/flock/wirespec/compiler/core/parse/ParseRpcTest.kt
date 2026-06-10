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
            |rpc CreateTodo(todo: Todo) -> Todo
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Rpc>()
            .run {
                comment?.value shouldBe null
                identifier.value shouldBe "CreateTodo"
                requestParameters shouldHaveSize 1
                requestParameters[0].identifier.value shouldBe "todo"
                requestParameters[0].reference.value shouldBe "Todo"
                response.value shouldBe "Todo"
                response.isNullable shouldBe false
            }
    }

    @Test
    fun testRpcParserMultipleParameters() {
        val source =
            // language=ws
            """
            |type Todo { name: String }
            |type TodoId { id: String }
            |rpc UpdateTodo(id: TodoId, todo: Todo) -> Todo
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(3)[2]
            .shouldBeInstanceOf<Rpc>()
            .run {
                identifier.value shouldBe "UpdateTodo"
                requestParameters shouldHaveSize 2
                requestParameters[0].identifier.value shouldBe "id"
                requestParameters[0].reference.value shouldBe "TodoId"
                requestParameters[1].identifier.value shouldBe "todo"
                requestParameters[1].reference.value shouldBe "Todo"
                response.value shouldBe "Todo"
            }
    }

    @Test
    fun testRpcParserWithError() {
        val source =
            // language=ws
            """
            |type Todo { name: String }
            |type Error { code: String }
            |rpc CreateTodo(todo: Todo) -> Todo ! Error
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(3)[2]
            .shouldBeInstanceOf<Rpc>()
            .run {
                identifier.value shouldBe "CreateTodo"
                response.value shouldBe "Todo"
                error?.value shouldBe "Error"
                error?.isNullable shouldBe false
            }
    }

    @Test
    fun testRpcParserWithoutErrorHasNullError() {
        val source =
            // language=ws
            """
            |type Todo { name: String }
            |rpc CreateTodo(todo: Todo) -> Todo
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Rpc>()
            .run { error shouldBe null }
    }

    @Test
    fun testRpcParserNoParametersAndUnitResponse() {
        val source =
            // language=ws
            """
            |rpc Ping() -> Unit
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(1)[0]
            .shouldBeInstanceOf<Rpc>()
            .run {
                identifier.value shouldBe "Ping"
                requestParameters shouldHaveSize 0
                response.shouldBeInstanceOf<Reference.Unit>()
            }
    }
}
