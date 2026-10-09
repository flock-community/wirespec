package community.flock.wirespec.compiler.core.parse

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.DefaultValue
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Identifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseTypeTest {

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source))).map { it.modules.flatMap(Module::statements) }

    @Test
    fun testTypeParser() {
        val source =
            // language=ws
            """
            |type Foo {
            |    bar: {String[]}
            |}
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Type>()
            .also { it.identifier.value shouldBe "Foo" }
            .shape.value
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Field>()
            .run {
                identifier.shouldBeInstanceOf<Identifier>().value shouldBe "bar"
                reference.shouldBeInstanceOf<Reference.Dict>()
                    .reference.shouldBeInstanceOf<Reference.Iterable>()
                    .reference.shouldBeInstanceOf<Reference.Primitive>().run {
                        type.shouldBeInstanceOf<Reference.Primitive.Type.String>()
                        isNullable.shouldBeFalse()
                        type.constraint shouldBe null
                    }
            }
    }

    @Test
    fun testAnyTypeParser() {
        val source =
            // language=ws
            """
            |type Foo {
            |    bar: Any,
            |    baz: Any?,
            |    qux: Any[]
            |}
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Type>()
            .also { it.identifier.value shouldBe "Foo" }
            .shape.value
            .shouldHaveSize(3)
            .run {
                get(0).reference.shouldBeInstanceOf<Reference.Any>().isNullable.shouldBeFalse()
                get(1).reference.shouldBeInstanceOf<Reference.Any>().isNullable shouldBe true
                get(2).reference.shouldBeInstanceOf<Reference.Iterable>()
                    .reference.shouldBeInstanceOf<Reference.Any>().isNullable.shouldBeFalse()
            }
    }

    @Test
    fun testRefinedParserString() {
        val source =
            // language=ws
            """
            |type DutchPostalCode = String(/^([0-9]{4}[A-Z]{2})$/g)
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "DutchPostalCode"
                reference.apply {
                    shouldBeInstanceOf<Reference.Primitive>()
                    type.shouldBeInstanceOf<Reference.Primitive.Type.String>()
                    isNullable.shouldBeFalse()
                    type.constraint shouldBe Reference.Primitive.Type.Constraint.RegExp("/^([0-9]{4}[A-Z]{2})$/g")
                }
            }
    }

    @Test
    fun testRefinedParserInteger() {
        val source =
            // language=ws
            """
            |type Age = Integer(0,99)
            """.trimMargin()

        parser(source)
            .shouldBeRight { it.head.message }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "Age"
                reference.apply {
                    isNullable shouldBe false
                    type.apply {
                        shouldBeInstanceOf<Reference.Primitive.Type.Integer>()
                        constraint?.min shouldBe "0"
                        constraint?.max shouldBe "99"
                    }
                }
            }
    }

    @Test
    fun testRefinedParserIntegerMinEmpty() {
        val source =
            // language=ws
            """
            |type Age = Integer(_,99)
            """.trimMargin()

        parser(source)
            .shouldBeRight { it.head.message }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "Age"
                reference.apply {
                    isNullable shouldBe false
                    type.apply {
                        shouldBeInstanceOf<Reference.Primitive.Type.Integer>()
                        constraint?.min shouldBe null
                        constraint?.max shouldBe "99"
                    }
                }
            }
    }

    @Test
    fun testRefinedParserNumber() {
        val source =
            // language=ws
            """
            |type Age = Number(0.0,9.9)
            """.trimMargin()

        parser(source)
            .shouldBeRight { it.head.message }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "Age"
                reference.apply {
                    isNullable shouldBe false
                    type.apply {
                        shouldBeInstanceOf<Reference.Primitive.Type.Number>()
                        constraint?.min shouldBe "0.0"
                        constraint?.max shouldBe "9.9"
                    }
                }
            }
    }

    @Test
    fun testUnionParser() {
        val source =
            // language=ws
            """
            |type Bar { str: String }
            |type Bal { str: String }
            |type Foo = Bar | Bal
            """.trimMargin()

        parser(source)
            .shouldBeRight()
            .shouldHaveSize(3)[2]
            .shouldBeInstanceOf<Union>()
            .also { it.identifier.value shouldBe "Foo" }
            .entries
            .shouldHaveSize(2)
            .let {
                val (first, second) = it.toList()
                first shouldBe Reference.Custom(value = "Bar", isNullable = false)
                second shouldBe Reference.Custom(value = "Bal", isNullable = false)
            }
    }

    @Test
    fun testIntegerNumberParser() {
        val source =
            // language=ws
            """
            |type Bar { int32: Integer32, int64: Integer[] }
            |type Foo { num32: Number32, num64: Number? }
            """.trimMargin()

        parser(source)
            .shouldBeRight { it.head.message }
            .shouldHaveSize(2)
            .let { (first, second) ->
                first shouldBe Type(
                    comment = null,
                    annotations = emptyList(),
                    identifier = DefinitionIdentifier("Bar"),
                    extends = emptyList(),
                    shape = Type.Shape(
                        value = listOf(
                            Field(
                                identifier = FieldIdentifier("int32"),
                                annotations = emptyList(),
                                reference = Reference.Primitive(
                                    type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32, null),
                                    isNullable = false,
                                ),
                            ),
                            Field(
                                identifier = FieldIdentifier("int64"),
                                annotations = emptyList(),
                                reference = Reference.Iterable(
                                    isNullable = false,
                                    reference = Reference.Primitive(
                                        type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P64, null),
                                        isNullable = false,
                                    ),
                                ),
                            ),
                        ),
                    ),
                )
                second shouldBe Type(
                    comment = null,
                    annotations = emptyList(),
                    identifier = DefinitionIdentifier("Foo"),
                    extends = emptyList(),
                    shape = Type.Shape(
                        value = listOf(
                            Field(
                                identifier = FieldIdentifier("num32"),
                                annotations = emptyList(),
                                reference = Reference.Primitive(
                                    type = Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P32, null),
                                    isNullable = false,
                                ),
                            ),
                            Field(
                                identifier = FieldIdentifier("num64"),
                                annotations = emptyList(),
                                reference = Reference.Primitive(
                                    type = Reference.Primitive.Type.Number(Reference.Primitive.Type.Precision.P64, null),
                                    isNullable = true,
                                ),
                            ),
                        ),
                    ),
                )
            }
    }

    @Test
    fun testDefaultValueParser() {
        val source =
            // language=ws
            """
            |type Foo {
            |    name: String = "Hello \"world\"",
            |    count: Integer = -1,
            |    port: Integer32(0, 65535) = 8080,
            |    ratio: Number = 1.5,
            |    scale: Number = 2,
            |    active: Boolean = false,
            |    nickname: String? = null,
            |    limit: Integer? = 10,
            |    tags: String[]
            |}
            """.trimMargin()

        parser(source)
            .shouldBeRight { it.head.message }
            .first()
            .shouldBeInstanceOf<Type>()
            .shape.value
            .map { it.identifier.value to it.defaultValue } shouldBe listOf(
            "name" to DefaultValue.StringValue("Hello \"world\""),
            "count" to DefaultValue.IntegerValue("-1"),
            "port" to DefaultValue.IntegerValue("8080"),
            "ratio" to DefaultValue.NumberValue("1.5"),
            "scale" to DefaultValue.NumberValue("2.0"),
            "active" to DefaultValue.BooleanValue(false),
            "nickname" to DefaultValue.NullValue,
            "limit" to DefaultValue.IntegerValue("10"),
            "tags" to null,
        )
    }

    @Test
    fun testEnumDefaultValue() {
        val source =
            // language=ws
            """
            |type Task {
            |    status: Status = DONE,
            |    priority: Priority? = 2,
            |    previous: Status? = null
            |}
            |enum Status { TODO, DONE }
            |enum Priority { 1, 2, 3 }
            """.trimMargin()

        parser(source)
            .shouldBeRight { it.head.message }
            .first()
            .shouldBeInstanceOf<Type>()
            .shape.value
            .map { it.defaultValue } shouldBe listOf(
            DefaultValue.EnumValue("DONE"),
            DefaultValue.EnumValue("2"),
            DefaultValue.NullValue,
        )
    }

    @Test
    fun testInvalidEnumDefaultValues() {
        listOf(
            "type Task { status: Status = DOING } enum Status { TODO, DONE }" to "Invalid default value DOING for field status of type Status",
            "type Task { status: Status = \"DONE\" } enum Status { TODO, DONE }" to "Invalid default value \"DONE\" for field status of type Status",
            "type Task { owner: User = DONE } type User { name: String }" to "Invalid default value DONE for field owner of type User",
            "type Task { name: String = DONE }" to "Invalid default value DONE for field name of type String",
        ).forEach { (source, message) ->
            parser(source).shouldBeLeft().head.message shouldBe message
        }
    }

    @Test
    fun testNullDefaultOnAnyNullableField() {
        val source =
            // language=ws
            """
            |type Bar { a: String }
            |type Foo {
            |    bar: Bar? = null,
            |    tags: String[]? = null,
            |    meta: { String }? = null
            |}
            """.trimMargin()

        parser(source)
            .shouldBeRight { it.head.message }
            .last()
            .shouldBeInstanceOf<Type>()
            .shape.value
            .map { it.defaultValue } shouldBe listOf(DefaultValue.NullValue, DefaultValue.NullValue, DefaultValue.NullValue)
    }

    @Test
    fun testInvalidDefaultValues() {
        listOf(
            "type Foo { name: String = 1 }" to "Invalid default value 1 for field name of type String",
            "type Foo { count: Integer = \"1\" }" to "Invalid default value \"1\" for field count of type Integer",
            "type Foo { count: Integer = 1.5 }" to "Invalid default value 1.5 for field count of type Integer",
            "type Foo { count: Integer32 = 2147483648 }" to "Invalid default value 2147483648 for field count of type Integer",
            "type Foo { count: Integer(0, 10) = 11 }" to "Invalid default value 11 for field count of type Integer",
            "type Foo { ratio: Number(0.0, 1.0) = 1.5 }" to "Invalid default value 1.5 for field ratio of type Number",
            "type Foo { active: Boolean = yes }" to "Invalid default value yes for field active of type Boolean",
            "type Foo { name: String = null }" to "Invalid default value null for field name of type String",
            "type Foo { tags: String[] = \"a\" }" to "Invalid default value \"a\" for field tags of type Iterable",
            "type Bar { a: String } type Foo { bar: Bar = \"a\" }" to "Invalid default value \"a\" for field bar of type Bar",
        ).forEach { (source, message) ->
            parser(source).shouldBeLeft().head.message shouldBe message
        }
    }

    @Test
    fun testDefaultValueOutsideTypeIsRejected() {
        val source =
            // language=ws
            """
            |endpoint GetTodos GET /todos ? {limit: Integer = 10} -> {
            |  200 -> String
            |}
            """.trimMargin()

        parser(source)
            .shouldBeLeft()
            .head.message shouldBe "Default values are only allowed on fields of a type definition, not on: limit"
    }
}
