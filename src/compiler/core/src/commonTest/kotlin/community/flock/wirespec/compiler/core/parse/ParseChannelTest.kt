package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse.Endpoint.Method.GET
import community.flock.wirespec.compiler.core.parse.Endpoint.Method.POST
import community.flock.wirespec.compiler.core.parse.Endpoint.Segment.Literal
import community.flock.wirespec.compiler.core.parse.Field.Reference
import community.flock.wirespec.compiler.core.parse.Field.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.Field.Reference.Primitive.Type.String
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseChannelTest {

    private fun parser() = Parser(noLogger)

    @Test
    fun testChannelParser() {
        val source = """
            type Todo { name: String }
            channel TodosChannel -> Todo
        """.trimIndent()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 2 }[1]
            .shouldBeInstanceOf<Channel>()
            .run {
                comment?.value shouldBe null
                identifier.value shouldBe "TodosChannel"
                reference.value shouldBe "Todo"
                isNullable shouldBe false
            }
    }

    @Test
    fun testChannelParserNullable() {
        val source = """
            type Todo { name: String }
            channel TodosChannel -> Todo?
        """.trimIndent()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 2 }[1]
            .shouldBeInstanceOf<Channel>()
            .run {
                comment?.value shouldBe null
                identifier.value shouldBe "TodosChannel"
                reference.value shouldBe "Todo"
                isNullable shouldBe true
            }
    }
}
