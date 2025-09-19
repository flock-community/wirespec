package community.flock.wirespec.compiler.core.parse

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldHaveSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ParseFieldAnnotationTest {

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test"), source))).map { it.modules.flatMap(Module::statements) }

    @Test
    fun testSimpleAnnotationOnField() {
        val source = """
            |type User {
            |  @Deprecated
            |  name: String
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight { it.joinToString { it.message } }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Type>()
            .apply {
                identifier.value shouldBe "User"
                shape.value.shouldHaveSize(1)
                shape.value.first().apply {
                    identifier.value shouldBe "name"
                    annotations.shouldHaveSize(1)
                    annotations.first().name shouldBe "Deprecated"
                }
            }
    }

    @Test
    fun testMultipleAnnotationsOnField() {
        val source = """
            |type User {
            |  @Deprecated
            |  @Internal
            |  name: String,
            |  age: Integer
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight { it.joinToString { it.message } }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Type>()
            .apply {
                identifier.value shouldBe "User"
                shape.value.shouldHaveSize(2)
                shape.value[0].apply {
                    identifier.value shouldBe "name"
                    annotations.shouldHaveSize(2)
                    annotations[0].name shouldBe "Deprecated"
                    annotations[1].name shouldBe "Internal"
                }
                shape.value[1].apply {
                    identifier.value shouldBe "age"
                    annotations.shouldHaveSize(0)
                }
            }
    }

    @Test
    fun testAnnotationWithParametersOnField() {
        val source = """
            |type User {
            |  @Validate(min: 1, max: 100)
            |  name: String
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight { it.joinToString { it.message } }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Type>()
            .apply {
                identifier.value shouldBe "User"
                shape.value.shouldHaveSize(1)
                shape.value.first().apply {
                    identifier.value shouldBe "name"
                    annotations.shouldHaveSize(1)
                    annotations.first().apply {
                        name shouldBe "Validate"
                        parameters.shouldHaveSize(2)
                        parameters[0].apply {
                            name shouldBe "min"
                            value shouldBe Annotation.Value.Single("1")
                        }
                        parameters[1].apply {
                            name shouldBe "max"
                            value shouldBe Annotation.Value.Single("100")
                        }
                    }
                }
            }
    }
}
