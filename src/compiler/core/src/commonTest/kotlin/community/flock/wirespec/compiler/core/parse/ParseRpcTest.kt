package community.flock.wirespec.compiler.core.parse

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.withoutRpcFields
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.DefaultValue
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Part
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.fields
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
                shape.fields.shouldHaveSize(2)
                shape.fields[0].identifier.value shouldBe "id"
                shape.fields[1].identifier.value shouldBe "verbose"
                result.shouldBeInstanceOf<Reference.Custom>().value shouldBe "Todo"
                error.shouldBeInstanceOf<Reference.Custom>().value shouldBe "TodoError"
            }
    }

    @Test
    fun testRpcParameterDefaults() {
        val source =
            // language=ws
            """
            |enum Order { ASC, DESC }
            |rpc ListTodos {
            |  limit: Integer? = 10,
            |  order: Order = ASC,
            |  query: String
            |} -> String[]
            """.trimMargin()

        parser(source)
            .shouldBeRight()[1]
            .shouldBeInstanceOf<Rpc>()
            .shape.fields.map { it.defaultValue } shouldBe listOf(DefaultValue.IntegerValue("10"), DefaultValue.EnumValue("ASC"), null)
    }

    @Test
    fun testInvalidRpcParameterDefaults() {
        mapOf(
            "rpc ListTodos { limit: Integer = \"ten\" } -> String[]" to "Invalid default value \"ten\" for field limit of type Integer",
            "enum Order { ASC, DESC } rpc ListTodos { order: Order = UP } -> String[]" to "Invalid default value UP for field order of type Order",
        ).forEach { (source, message) ->
            parser(source).shouldBeLeft().head.message shouldBe message
        }
    }

    @Test
    fun testFieldThatRefersToAnRpc() {
        val source =
            // language=ws
            """
            |type Todo { name: String, comments: TodoComments }
            |part Paged { next: TodoComments }
            |rpc TodoComments { first: Integer? = 10 } -> String[]
            """.trimMargin()

        val ast = object : ParseContext, NoLogger {
            override val spec = WirespecSpec
        }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source))).shouldBeRight()

        ast.modules.head.statements.head.shouldBeInstanceOf<Type>().shape.fields.map { it.identifier.value } shouldBe listOf("name", "comments")
        ast.withoutRpcFields().modules.head.statements.toList().take(2).map { definition ->
            when (definition) {
                is Type -> definition.shape.fields
                is Part -> definition.shape.fields
                else -> emptyList()
            }.map { it.identifier.value }
        } shouldBe listOf(listOf("name"), emptyList())
    }

    @Test
    fun testRpcReferenceThatIsNoField() {
        mapOf(
            "type Todo { comments: TodoComments[] } rpc TodoComments {} -> String" to "Todo cannot refer to rpc TodoComments here",
            "type Todo { comments: TodoComments? } rpc TodoComments {} -> String" to "Todo cannot refer to rpc TodoComments here",
            "type Todo { comments: { TodoComments } } rpc TodoComments {} -> String" to "Todo cannot refer to rpc TodoComments here",
            "rpc Search { by: TodoComments } -> String rpc TodoComments {} -> String" to "Search cannot refer to rpc TodoComments here",
            "rpc Search {} -> TodoComments rpc TodoComments {} -> String" to "Search cannot refer to rpc TodoComments here",
        ).forEach { (source, message) ->
            parser(source).shouldBeLeft().head.message shouldBe "$message: an rpc can only be the type of a field of a type or part, as in `name: TodoComments`"
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
                shape.fields.shouldHaveSize(1)
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
                shape.fields.shouldHaveSize(0)
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
