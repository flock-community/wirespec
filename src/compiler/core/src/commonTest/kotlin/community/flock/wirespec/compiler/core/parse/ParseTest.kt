package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ParseTest {

    private fun parser() = Parser(noLogger)

    @Test
    fun testParserWithCorrectInput() {
        val source = """
            |type Bla {
            |  foo: String,
            |  bar: String
            |}

        """.trimMargin()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .size shouldBe 1
    }

    @Test
    fun testParserWithDigits() {
        val source = """
            |type Bla {
            |  foo1: String,
            |  bar2: String
            |}

        """.trimMargin()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .size shouldBe 1
    }

    @Test
    fun testParserWithFaultyInput() {
        val source = """
            |type Bla {
            |  foo: String
            |  bar: String
            |}

        """.trimMargin()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeLeft()
            .also { it.size shouldBe 1 }
            .first()
            .message shouldBe "RightCurly expected, not: CustomValue at line 3 and position 3"
    }

    @Test
    fun testParserWithComment() {
        val source = """
            |/**
            | * This is a comment
            | */
            |type Bla {
            |  foo: String,
            |  bar: String
            |}

        """.trimMargin()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .size shouldBe 1
    }

    @Test
    fun testParserWithDoubleComment() {
        val source = """
            |/**
            | * This is comment 1
            | */
            |type Foo {
            |  foo: String
            |}
            |
            |/**
            | * This is comment 2
            | */
            |type Bar {
            |  bar: String
            |}
        """.trimMargin()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight().filterIsInstance<Definition>().map { it.comment?.value } shouldBe listOf(
            "/**\n * This is comment 1\n */",
            "/**\n * This is comment 2\n */",
        )
    }
}
