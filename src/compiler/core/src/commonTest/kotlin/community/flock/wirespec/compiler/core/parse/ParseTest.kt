package community.flock.wirespec.compiler.core.parse

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ParseTest {

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent("", source))).map { it.modules.flatMap(Module::statements) }

    @Test
    fun testParserWithCorrectInput() {
        val source = """
            |type Bla {
            |  foo: String,
            |  bar: String
            |}

        """.trimMargin()

        parser(source)
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

        parser(source)
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

        parser(source)
            .shouldBeLeft()
            .also { it.size shouldBe 1 }
            .first()
            .message shouldBe "RightCurly expected, not: DromedaryCaseIdentifier at line 3 and position 3"
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

        parser(source)
            .shouldBeRight()
            .size shouldBe 1
    }

    @Test
    fun testParserWithDoubleComment() {
        val source = """
            |/*
            | This is comment 1
            | */
            |type Foo {
            |  foo: String
            |}
            |
            |/*
            | This is comment 2
            | */
            |type Bar {
            |  bar: String
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight().filterIsInstance<Definition>().map { it.comment?.value } shouldBe listOf(
            "This is comment 1",
            "This is comment 2",
        )
    }

    @Test
    fun testParserCommentRefinedType() {
        val source = """
            |/*
            |  comment Name
            |  */
            |type Name -> String(/^[0-9a-zA-Z]{1,50}$/g)
            |/*
            |  comment Address
            |  */
            |type Address {
            |  street: Name?,
            |  houseNumber: Integer
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight().filterIsInstance<Definition>().map { it.comment?.value } shouldBe listOf(
            "comment Name",
            "comment Address",
        )
    }
}
