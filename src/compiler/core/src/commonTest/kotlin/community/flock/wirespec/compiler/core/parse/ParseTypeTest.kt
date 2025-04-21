package community.flock.wirespec.compiler.core.parse

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseTypeTest {

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent("", source))).map { it.modules.flatMap(Module::statements) }

    @Test
    fun testTypeParser() {
        val source = """
            |type Foo {
            |    bar: {String[]}
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .also { it.size shouldBe 1 }
            .first()
            .shouldBeInstanceOf<Type>()
            .also { it.identifier.value shouldBe "Foo" }
            .shape.value
            .also { it.size shouldBe 1 }
            .first()
            .shouldBeInstanceOf<Field>()
            .run {
                identifier.shouldBeInstanceOf<Identifier>().value shouldBe "bar"
                reference.shouldBeInstanceOf<Reference.Dict>()
                    .reference.shouldBeInstanceOf<Reference.Iterable>()
                    .reference.shouldBeInstanceOf<Reference.Primitive>().run {
                        type.shouldBeInstanceOf<Reference.Primitive.Type.String>()
                        isNullable.shouldBeFalse()
                    }
            }
    }

    @Test
    fun testRefinedParser() {
        val source = """
            |type DutchPostalCode /^([0-9]{4}[A-Z]{2})$/g
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .also { it.size shouldBe 1 }
            .first()
            .shouldBeInstanceOf<Refined>()
            .also { it.identifier.value shouldBe "DutchPostalCode" }
            .validator.shouldBeInstanceOf<Refined.Validator>()
            .value shouldBe "/^([0-9]{4}[A-Z]{2})$/g"
    }

    @Test
    fun testUnionParser() {
        val source = """
            |type Bar { str: String }
            |type Bal { str: String }
            |type Foo = Bar | Bal
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .also { it.size shouldBe 3 }[2]
            .shouldBeInstanceOf<Union>()
            .also { it.identifier.value shouldBe "Foo" }
            .entries
            .also { it.size shouldBe 2 }
            .let {
                val (first, second) = it.toList()
                first shouldBe Reference.Custom(value = "Bar", isNullable = false)
                second shouldBe Reference.Custom(value = "Bal", isNullable = false)
            }
    }

    @Test
    fun testIntegerNumberParser() {
        val source = """
            |type Bar { int32: Integer32, int64: Integer[] }
            |type Foo { num32: Number32, num64: Number? }
        """.trimMargin()

        parser(source)
            .shouldBeRight()
            .also { it.size shouldBe 2 }
            .let { (first, second) ->
                first shouldBe Type(
                    comment = null,
                    identifier = DefinitionIdentifier("Bar"),
                    extends = emptyList(),
                    shape = Type.Shape(
                        value = listOf(
                            Field(
                                identifier = FieldIdentifier("int32"),
                                reference = Reference.Primitive(
                                    type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32),
                                    isNullable = false,
                                ),
                            ),
                            Field(
                                identifier = FieldIdentifier("int64"),
                                reference = Reference.Iterable(
                                    isNullable = false,
                                    reference = Reference.Primitive(
                                        type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P64),
                                        isNullable = false,
                                    ),
                                ),
                            ),
                        ),
                    ),
                )
                second shouldBe Type(
                    comment = null,
                    identifier = DefinitionIdentifier("Foo"),
                    extends = emptyList(),
                    shape = Type.Shape(
                        value = listOf(
                            Field(
                                identifier = FieldIdentifier("num32"),
                                reference = Reference.Primitive(
                                    type = Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P32),
                                    isNullable = false,
                                ),
                            ),
                            Field(
                                identifier = FieldIdentifier("num64"),
                                reference = Reference.Primitive(
                                    type = Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P64),
                                    isNullable = true,
                                ),
                            ),
                        ),
                    ),
                )
            }
    }
}
