package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseChannelTest {

    private fun parser(source: String) = object : ParseContext {
        override val spec = WirespecSpec
        override val logger = noLogger
    }.parse(source)

    @Test
    fun testChannelParser() {
        val source = """
            |type Todo { name: String }
            |channel TodosChannel -> Todo
        """.trimMargin()

        parser(source)
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

        parser(source)
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
