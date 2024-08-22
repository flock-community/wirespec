package community.flock.wirespec.compiler.core.parse

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.tokenize.tokenize
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseTypeTest {

    private fun parser() = Parser(noLogger)

    @Test
    fun testTypeParser() {
        val source = """
            |type Foo {
            |    bar: {String[]}
            |}
        """.trimMargin()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
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
                reference.shouldBeInstanceOf<Reference.Primitive>().run {
                    type shouldBe Reference.Primitive.Type.String
                    isIterable shouldBe true
                    isDictionary shouldBe true
                }
                isNullable shouldBe false
            }

    }

    @Test
    fun testRefinedParser() {
        val source = """
            |type DutchPostalCode /^([0-9]{4}[A-Z]{2})$/g
        """.trimMargin()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
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

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 3 }[2]
            .shouldBeInstanceOf<Union>()
            .also { it.identifier.value shouldBe "Foo" }
            .entries
            .also { it.size shouldBe 2 }
            .let {
                val (first, second) = it.toList()
                first shouldBe Reference.Custom(value = "Bar", isIterable = false, isDictionary = false)
                second shouldBe Reference.Custom(value = "Bal", isIterable = false, isDictionary = false)
            }
    }
}
