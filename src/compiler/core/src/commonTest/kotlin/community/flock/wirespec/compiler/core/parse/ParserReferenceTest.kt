package community.flock.wirespec.compiler.core.parse

import arrow.core.nonEmptyListOf
import arrow.core.right
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParserReferenceTest {

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent("", source))).map { it.modules.flatMap(Module::statements) }

    @Test
    fun shouldHaveSelfRef() {
        val source = """
            |type Self {
            |  self: Self
            |}

        """.trimMargin()

        parser(source)
            .shouldBeRight { it.head.message }
            .apply { size shouldBe 1 }
            .apply {
                first().apply {
                    shouldBeInstanceOf<Type>()
                    identifier.value shouldBe "Self"
                    shape.value.first().apply {
                        identifier.value shouldBe "self"
                        reference.shouldBeInstanceOf<Reference.Custom>()
                        reference.value shouldBe "Self"
                        reference.isNullable shouldBe false
                    }
                }
            }
    }

    @Test
    fun shouldNotFindReferenceInType() {
        val source = """
            |type Foo {
            |  bar: Bar
            |}
        """.trimMargin()

        parser(source)
            .shouldBeLeft()
            .also { it.size shouldBe 1 }
            .first()
            .run {
                message shouldBe "Cannot find reference: Bar"
                coordinates.line shouldBe 2
                coordinates.position shouldBe 11
                coordinates.idxAndLength.idx shouldBe 21
                coordinates.idxAndLength.length shouldBe 3
            }
    }

    @Test
    fun shouldNotFindReferenceInEndpointRequest() {
        val source = """
            |endpoint FooPoint POST Foo /foo -> {
            |  200 -> Bar
            |}
        """.trimMargin()

        parser(source)
            .shouldBeLeft()
            .apply { size shouldBe 1 }
            .apply {
                first().message shouldBe "Cannot find reference: Foo"
                first().coordinates.line shouldBe 1
                first().coordinates.position shouldBe 27
                first().coordinates.idxAndLength.idx shouldBe 26
                first().coordinates.idxAndLength.length shouldBe 3
            }
    }

    @Test
    fun shouldNotFindReferenceInEndpointResponse() {
        val source = """
            |type Foo { str:String }
            |endpoint FooPoint POST Foo /foo -> {
            |  200 -> Bar
            |}
        """.trimMargin()

        val r = parser(source).right()
        println(r)
        parser(source)
            .shouldBeLeft()
            .apply { size shouldBe 1 }
            .apply {
                first().message shouldBe "Cannot find reference: Bar"
                first().coordinates.line shouldBe 3
                first().coordinates.position shouldBe 13
                first().coordinates.idxAndLength.idx shouldBe 73
                first().coordinates.idxAndLength.length shouldBe 3
            }
    }

    @Test
    fun shouldNotFindReferenceInEnum() {
        val source = """
            |type Foo = Bar
        """.trimMargin()

        parser(source)
            .shouldBeLeft()
            .apply { size shouldBe 1 }
            .apply {
                first().message shouldBe "Cannot find reference: Bar"
                first().coordinates.line shouldBe 1
                first().coordinates.position shouldBe 15
                first().coordinates.idxAndLength.idx shouldBe 14
                first().coordinates.idxAndLength.length shouldBe 3
            }
    }

    @Test
    fun shouldNotFindReferenceInEnumSecond() {
        val source = """
            |type Bar { str:String }
            |type Foo = Bar | Baz
        """.trimMargin()

        parser(source)
            .shouldBeLeft()
            .apply { size shouldBe 1 }
            .apply {
                first().message shouldBe "Cannot find reference: Baz"
                first().coordinates.line shouldBe 2
                first().coordinates.position shouldBe 21
                first().coordinates.idxAndLength.idx shouldBe 44
                first().coordinates.idxAndLength.length shouldBe 3
            }
    }

    @Test
    fun singleLine() {
        val source = """
            |// This is a comment
            |type Address {
            |  houseNumber: Integer
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .apply { first().comment?.value shouldBe "This is a comment" }
    }

    @Test
    fun multiLine() {
        val source = """
            |/* This is a comment */
            |type Address {
            |  houseNumber: Integer
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .apply { first().comment?.value shouldBe "This is a comment" }
    }

    @Test
    fun afterRegex() {
        val source = """
            |/* This is a comment */
            |type Date -> String(/^([0-9]{2}-[0-9]{2}-20[0-9]{2})$/g)
            |
            |// This is a comment too
            |type Address {
            |  houseNumber: Integer
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .apply { first().comment?.value shouldBe "This is a comment" }
            .apply { first().identifier.value shouldBe "Date" }
            .apply { (first() as Refined).validator.value shouldBe "/^([0-9]{2}-[0-9]{2}-20[0-9]{2})\$/g" }
            .apply { get(1).comment?.value shouldBe "This is a comment too" }
            .apply { get(1).identifier.value shouldBe "Address" }
            .apply { get(1).shouldBeInstanceOf<Type>() }
    }

    // region NOT YET SUPPORTED

    @Test
    fun loseComments() {
        val source = "// This is a comment"

        parser(source)
            .shouldBeLeft()
    }

    @Test
    fun fileEndingComments() {
        val source = """
            |type Address {
            |  houseNumber: Integer
            |}
            |// This is a comment"
        """.trimMargin()

        parser(source)
            .shouldBeLeft()
    }

    @Test
    fun commentsInDefinitions() {
        val source = """
            |type Address { // This is a comment
            |  houseNumber: Integer
            |}
        """.trimMargin()

        parser(source)
            .shouldBeLeft()
    }

    // endregion
}
