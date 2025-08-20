package community.flock.wirespec.compiler.core.parse

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ParseMultipleErrorsTest {

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source))).map { it.modules.flatMap(Module::statements) }

    @Test
    fun testParserWithCorrectInput() {
        val source = """
            |type Foo {
            |  foo: String,
            |  bar: String,
            |}
            |
            |type Bar {
            |  foo: String,
            |  bar: String,
            |}
        """.trimMargin()

        parser(source)
            .shouldBeLeft()
            .shouldHaveSize(2)
            .apply {
                get(0).message shouldBe "WirespecIdentifier expected, not: RightCurly at line 4 and position 1"
                get(1).message shouldBe "WirespecIdentifier expected, not: RightCurly at line 9 and position 1"
            }
    }
}
