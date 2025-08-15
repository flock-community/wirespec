package community.flock.wirespec.compiler.core.parse

import arrow.core.nonEmptyListOf
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

class ParseAnnotationTest {

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent("", source))).map { it.modules.flatMap(Module::statements) }

    @Test
    fun testSimpleAnnotationOnType() {
        val source = """
            |@deprecated
            |type User = String
        """.trimMargin()

        parser(source)
            .shouldBeRight { it.joinToString { it.message } }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "User"
                annotations.shouldHaveSize(1)
                annotations.first().apply {
                    name shouldBe "deprecated"
                    parameters shouldHaveSize 0
                }
            }
    }

    @Test
    fun testMultipleAnnotationsOnType() {
        val source = """
            |@deprecated
            |@internal
            |type User {
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
                annotations.shouldHaveSize(2)
                annotations[0].name shouldBe "deprecated"
                annotations[1].name shouldBe "internal"
            }
    }

    @Test
    fun testAnnotationWithParametersOnType() {
        val source = """
            |@since("1.0.0")
            |type User = String
        """.trimMargin()

        parser(source)
            .shouldBeRight { it.joinToString { it.message } }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "User"
                annotations.shouldHaveSize(1)
                annotations.first().apply {
                    name shouldBe "since"
                    parameters.shouldHaveSize(1)
                    parameters.first().apply {
                        name shouldBe null // positional parameter
                        value shouldBe "\"1.0.0\""
                    }
                }
            }
    }

    @Test
    fun testAnnotationWithNamedParametersOnType() {
        val source = """
            |@validate(min: 0, max: 100)
            |type Age = Integer
        """.trimMargin()

        parser(source)
            .shouldBeRight { it.joinToString { it.message } }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "Age"
                annotations.shouldHaveSize(1)
                annotations.first().apply {
                    name shouldBe "validate"
                    parameters.shouldHaveSize(2)
                    parameters[0].apply {
                        name shouldBe "min"
                        value shouldBe "0"
                    }
                    parameters[1].apply {
                        name shouldBe "max"
                        value shouldBe "100"
                    }
                }
            }
    }

    @Test
    fun testAnnotationOnEnum() {
        val source = """
            |@deprecated
            |enum Status {
            |  ACTIVE,
            |  INACTIVE
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight { it.joinToString { it.message } }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Enum>()
            .apply {
                identifier.value shouldBe "Status"
                annotations.shouldHaveSize(1)
                annotations.first().name shouldBe "deprecated"
            }
    }

    @Test
    fun testAnnotationOnEndpoint() {
        val source = """
            |@authenticated
            |endpoint GetUser GET /user/{id:String} -> {
            |   200 -> String
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight { it.joinToString { it.message } }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Endpoint>()
            .apply {
                identifier.value shouldBe "GetUser"
                annotations.shouldHaveSize(1)
                annotations.first().name shouldBe "authenticated"
                method shouldBe Endpoint.Method.GET
            }
    }

    @Test
    fun testAnnotationOnChannel() {
        val source = """
            |@secured
            |channel UserUpdates -> String
        """.trimMargin()

        parser(source)
            .shouldBeRight { it.joinToString { it.message } }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Channel>()
            .apply {
                identifier.value shouldBe "UserUpdates"
                annotations.shouldHaveSize(1)
                annotations.first().name shouldBe "secured"
            }
    }

    @Test
    fun testComplexAnnotationWithMixedParameters() {
        val source = """
            |@config("development", env: "test", debug: true)
            |type Config = String(/^([0-9]{2}-[0-9]{2}-20[0-9]{2})$/g)
        """.trimMargin()

        parser(source)
            .shouldBeRight { it.joinToString { it.message } }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "Config"
                annotations.shouldHaveSize(1)
                annotations.first().apply {
                    name shouldBe "config"
                    parameters.shouldHaveSize(3)
                    parameters[0].apply {
                        name shouldBe null // positional
                        value shouldBe "\"development\""
                    }
                    parameters[1].apply {
                        name shouldBe "env"
                        value shouldBe "\"test\""
                    }
                    parameters[2].apply {
                        name shouldBe "debug"
                        value shouldBe "true"
                    }
                }
            }
    }

    @Test
    fun testAnnotationWithCommentOnType() {
        val source = """
            |@deprecated
            |/*
            | * This is a user type
            | */
            |type User = String
        """.trimMargin()

        parser(source)
            .shouldBeRight { it.joinToString { it.message } }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "User"
                annotations.shouldHaveSize(1)
                annotations.first().name shouldBe "deprecated"
                comment?.value shouldBe """
                    |* This is a user type
                """.trimMargin()
            }
    }

    @Test
    fun testMultipleAnnotationsWithParametersOnEndpoint() {
        val source = """
            |@rateLimit(requests: 100, window: 60)
            |@authenticated
            |endpoint GetUsers GET /users -> {
            | 200 -> String
            |}
        """.trimMargin()

        parser(source)
            .shouldBeRight { it.joinToString { it.message } }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Endpoint>()
            .apply {
                identifier.value shouldBe "GetUsers"
                annotations.shouldHaveSize(2)
                annotations[0].apply {
                    name shouldBe "rateLimit"
                    parameters.shouldHaveSize(2)
                    parameters[0].apply {
                        name shouldBe "requests"
                        value shouldBe "100"
                    }
                    parameters[1].apply {
                        name shouldBe "window"
                        value shouldBe "60"
                    }
                }
                annotations[1].apply {
                    name shouldBe "authenticated"
                    parameters shouldHaveSize 0
                }
            }
    }

    @Test
    fun testAnnotationWithEmptyParametersOnType() {
        val source = """
            |@experimental()
            |type NewFeature = String
        """.trimMargin()

        parser(source)
            .shouldBeRight { it.joinToString { it.message } }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "NewFeature"
                annotations.shouldHaveSize(1)
                annotations.first().apply {
                    name shouldBe "experimental"
                    parameters shouldHaveSize 0
                }
            }
    }
}
