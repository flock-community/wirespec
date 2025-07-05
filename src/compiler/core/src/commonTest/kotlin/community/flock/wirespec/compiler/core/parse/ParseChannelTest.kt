package community.flock.wirespec.compiler.core.parse

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseChannelTest {

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent("", source))).map { it.modules.flatMap(Module::statements) }

    @Test
    fun testChannelParser() {
        val source = """
            |type Todo { name: String }
            |channel TodosChannel -> Todo
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Channel>()
            .run {
                comment?.value shouldBe null
                identifier.value shouldBe "TodosChannel"
                reference.value shouldBe "Todo"
                reference.isNullable shouldBe false
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
            .shouldHaveSize(2)[1]
            .shouldBeInstanceOf<Channel>()
            .run {
                comment?.value shouldBe null
                identifier.value shouldBe "TodosChannel"
                reference.value shouldBe "Todo"
                reference.isNullable shouldBe true
            }
    }
}
