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
            type Foo {
                bar: String
            }
        """.trimIndent()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 1 }
            .first()
            .shouldBeInstanceOf<Type>()
            .also { it.name shouldBe "Foo" }
            .shape.value
            .also { it.size shouldBe 1 }
            .first()
            .shouldBeInstanceOf<Type.Shape.Field>()
            .run {
                identifier.shouldBeInstanceOf<Type.Shape.Field.Identifier>().value shouldBe "bar"
                reference.shouldBeInstanceOf<Type.Shape.Field.Reference.Primitive>().run {
                    type shouldBe Type.Shape.Field.Reference.Primitive.Type.String
                    isIterable shouldBe false
                    isMap shouldBe false
                }
                isNullable shouldBe false
            }

    }

    @Test
    fun testRefinedParser() {
        val source = """
            type DutchPostalCode /^([0-9]{4}[A-Z]{2})$/g
        """.trimIndent()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 1 }
            .first()
            .shouldBeInstanceOf<Refined>()
            .also { it.name shouldBe "DutchPostalCode" }
            .validator.shouldBeInstanceOf<Refined.Validator>()
            .value shouldBe "/^([0-9]{4}[A-Z]{2})$/g"
    }

    @Test
    fun testUnionParser() {
        val source = """
            type Foo = Bar | Bal
        """.trimIndent()

        WirespecSpec.tokenize(source)
            .let(parser()::parse)
            .shouldBeRight()
            .also { it.size shouldBe 1 }
            .first()
            .shouldBeInstanceOf<Union>()
            .also { it.name shouldBe "Foo" }
            .entries
            .also { it.size shouldBe 2 }
            .let {
                val (first, second) = it.toList()
                first shouldBe "Bar"
                second shouldBe "Bal"
            }
    }
}
