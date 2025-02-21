package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseImportTest {

    private fun parser(source: String) = object : ParseContext {
        override val spec = WirespecSpec
        override val logger = noLogger
    }.parse(source)

    @Test
    fun testImportParser() {
        val source = """
            |import { Bar } "bar.ws"
            |type Foo {
            |    bar: Bar
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .also { it.size shouldBe 2 }[0]
            .shouldBeInstanceOf<Import>()
            .run {
                url.value shouldBe "bar.ws"
                references[0] shouldBe Reference.Custom(value = "Bar", isNullable = false)
            }
    }

    @Test
    fun testRelativePathParser() {
        val source = """
            |import { Bar } "/tmp/bar.ws"
            |type Foo {
            |    bar: Bar
            |}
        """.trimMargin()

        parser(source)
            .shouldBeLeft()
            .run {
                first().message shouldBe "Can only import relative paths in the same directory as the source file"
            }
    }

    @Test
    fun testMultipleImportParser() {
        val source = """
            |import { Foo, Bar } "foobar.ws"
            |type FooBar {
            |    foo: Foo,
            |    bar: Bar
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .also { it.size shouldBe 2 }[0]
            .shouldBeInstanceOf<Import>()
            .run {
                url.value shouldBe "foobar.ws"
                references[0] shouldBe Reference.Custom(value = "Foo", isNullable = false)
                references[1] shouldBe Reference.Custom(value = "Bar", isNullable = false)
            }
    }

    @Test
    fun testEmptyImportsParser() {
        val source = """
            |import {} "bar.ws"
        """.trimMargin()

        parser(source)
            .shouldBeLeft()
            .run {
                first().message shouldBe "List of imports cannot be empty"
            }
    }
}
