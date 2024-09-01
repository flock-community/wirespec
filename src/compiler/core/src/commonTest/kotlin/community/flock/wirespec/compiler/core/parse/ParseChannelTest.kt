package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseChannelTest {

    private fun parser() = Parser(noLogger)

    @Test
    fun testChannelParser() {
        val source = """
            |type Todo { name: String }
            |channel TodosChannel -> Todo
        """.trimMargin()

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
            |type Todo { name: String }
            |channel TodosChannel -> Todo?
        """.trimMargin()

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
