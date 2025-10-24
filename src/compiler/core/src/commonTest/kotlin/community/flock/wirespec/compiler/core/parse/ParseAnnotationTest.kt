package community.flock.wirespec.compiler.core.parse

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
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
    }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).map { it.modules.flatMap(Module::statements) }

    @Test
    fun testSimpleAnnotationOnType() {
        val source =
            // language=ws
            """
            |@Deprecated
            |type User = String
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "User"
                annotations.shouldHaveSize(1)
                annotations.first().apply {
                    name shouldBe "Deprecated"
                    parameters shouldHaveSize 0
                }
            }
    }

    @Test
    fun testMultipleAnnotationsOnType() {
        val source =
            // language=ws
            """
            |@Deprecated
            |@Internal
            |type User {
            |  name: String
            |}
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Type>()
            .apply {
                identifier.value shouldBe "User"
                annotations.shouldHaveSize(2)
                annotations[0].name shouldBe "Deprecated"
                annotations[1].name shouldBe "Internal"
            }
    }

    @Test
    fun testAnnotationWithParametersOnType() {
        val source =
            // language=ws
            """
            |@Since("1.0.0")
            |type User = String
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "User"
                annotations.shouldHaveSize(1)
                annotations.first().apply {
                    name shouldBe "Since"
                    parameters.shouldHaveSize(1)
                    parameters.first().apply {
                        name shouldBe "default" // positional parameter
                        val v = value.shouldBeInstanceOf<Annotation.Value.Single>()
                        v.value shouldBe "1.0.0"
                    }
                }
            }
    }

    @Test
    fun testAnnotationWithNamedParametersOnType() {
        val source =
            // language=ws
            """
            |@Validate(min: 0, max: 100)
            |type Age = Integer
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "Age"
                annotations.shouldHaveSize(1)
                annotations.first().apply {
                    name shouldBe "Validate"
                    parameters.shouldHaveSize(2)
                    parameters[0].apply {
                        name shouldBe "min"
                        val v = value.shouldBeInstanceOf<Annotation.Value.Single>()
                        v.value shouldBe "0"
                    }
                    parameters[1].apply {
                        name shouldBe "max"
                        val v = value.shouldBeInstanceOf<Annotation.Value.Single>()
                        v.value shouldBe "100"
                    }
                }
            }
    }

    @Test
    fun testAnnotationOnEnum() {
        val source =
            // language=ws
            """
            |@Deprecated
            |enum Status {
            |  ACTIVE,
            |  INACTIVE
            |}
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Enum>()
            .apply {
                identifier.value shouldBe "Status"
                annotations.shouldHaveSize(1)
                annotations.first().name shouldBe "Deprecated"
            }
    }

    @Test
    fun testAnnotationOnEndpoint() {
        val source =
            // language=ws
            """
            |@Authenticated
            |endpoint GetUser GET /user/{id:String} -> {
            |   200 -> String
            |}
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Endpoint>()
            .apply {
                identifier.value shouldBe "GetUser"
                annotations.shouldHaveSize(1)
                annotations.first().name shouldBe "Authenticated"
                method shouldBe Endpoint.Method.GET
            }
    }

    @Test
    fun testAnnotationOnChannel() {
        val source =
            // language=ws
            """
            |@Secured
            |channel UserUpdates -> String
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Channel>()
            .apply {
                identifier.value shouldBe "UserUpdates"
                annotations.shouldHaveSize(1)
                annotations.first().name shouldBe "Secured"
            }
    }

    @Test
    fun testComplexAnnotationWithMixedParameters() {
        val source =
            // language=ws
            """
            |@Config("development", env: "test", debug: true)
            |type Config = String(/^([0-9]{2}-[0-9]{2}-20[0-9]{2})$/g)
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "Config"
                annotations.shouldHaveSize(1)
                annotations.first().apply {
                    name shouldBe "Config"
                    parameters.shouldHaveSize(3)
                    parameters[0].apply {
                        name shouldBe "default" // positional
                        val v0 = value.shouldBeInstanceOf<Annotation.Value.Single>()
                        v0.value shouldBe "development"
                    }
                    parameters[1].apply {
                        name shouldBe "env"
                        val v1 = value.shouldBeInstanceOf<Annotation.Value.Single>()
                        v1.value shouldBe "test"
                    }
                    parameters[2].apply {
                        name shouldBe "debug"
                        val v2 = value.shouldBeInstanceOf<Annotation.Value.Single>()
                        v2.value shouldBe "true"
                    }
                }
            }
    }

    @Test
    fun testAnnotationWithCommentOnType() {
        val source =
            // language=ws
            """
            |@Deprecated
            |/*
            | * This is a user type
            | */
            |type User = String
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "User"
                annotations.shouldHaveSize(1)
                annotations.first().name shouldBe "Deprecated"
                comment?.value shouldBe "* This is a user type"
            }
    }

    @Test
    fun testMultipleAnnotationsWithParametersOnEndpoint() {
        val source =
            // language=ws
            """
            |@RateLimit(requests: 100, window: 60)
            |@Authenticated
            |endpoint GetUsers GET /users -> {
            | 200 -> String
            |}
            """.trimMargin()

        parser(source)
            .shouldBeRight { it -> it.joinToString { it.message } }
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Endpoint>()
            .apply {
                identifier.value shouldBe "GetUsers"
                annotations.shouldHaveSize(2)
                annotations[0].apply {
                    name shouldBe "RateLimit"
                    parameters.shouldHaveSize(2)
                    parameters[0].apply {
                        name shouldBe "requests"
                        val v0 = value.shouldBeInstanceOf<Annotation.Value.Single>()
                        v0.value shouldBe "100"
                    }
                    parameters[1].apply {
                        name shouldBe "window"
                        val v1 = value.shouldBeInstanceOf<Annotation.Value.Single>()
                        v1.value shouldBe "60"
                    }
                }
                annotations[1].apply {
                    name shouldBe "Authenticated"
                    parameters shouldHaveSize 0
                }
            }
    }

    @Test
    fun testAnnotationWithEmptyParametersOnType() {
        val source =
            // language=ws
            """
            |@Experimental()
            |type NewFeature = String
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "NewFeature"
                annotations.shouldHaveSize(1)
                annotations.first().apply {
                    name shouldBe "Experimental"
                    parameters shouldHaveSize 0
                }
            }
    }

    @Test
    fun testAnnotationWithUnnamedArrayParameter() {
        val source =
            // language=ws
            """
            |@Tag(["TagA", "TagB"])
            |endpoint GetUser GET /user/{id:String} -> {
            |   200 -> String
            |}
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Endpoint>()
            .apply {
                identifier.value shouldBe "GetUser"
                annotations.shouldHaveSize(1)
                annotations.first().apply {
                    name shouldBe "Tag"
                    parameters.shouldHaveSize(1)
                    parameters[0].apply {
                        name shouldBe "default"
                        val arr = value.shouldBeInstanceOf<Annotation.Value.Array>()
                        arr.value.map { it.value } shouldBe listOf("TagA", "TagB")
                    }
                }
            }
    }

    @Test
    fun testAnnotationWithNamedArrayParameter() {
        val source =
            // language=ws
            """
            |@Security(roles: ["RoleA", "RoleB"])
            |endpoint GetUser GET /user/{id:String} -> {
            |   200 -> String
            |}
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Endpoint>()
            .apply {
                identifier.value shouldBe "GetUser"
                annotations.shouldHaveSize(1)
                annotations.first().apply {
                    name shouldBe "Security"
                    parameters.shouldHaveSize(1)
                    parameters[0].apply {
                        name shouldBe "roles"
                        val arr = value.shouldBeInstanceOf<Annotation.Value.Array>()
                        arr.value.map { it.value } shouldBe listOf("RoleA", "RoleB")
                    }
                }
            }
    }

    @Test
    fun testAnnotationWithMixedArrayAndSingleParameters() {
        val source =
            // language=ws
            """
            |@Config(environment: "dev", tags: ["tag1", "tag2"], debug: true)
            |type AppConfig = String
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Refined>()
            .apply {
                identifier.value shouldBe "AppConfig"
                annotations.shouldHaveSize(1)
                annotations.first().apply {
                    name shouldBe "Config"
                    parameters.shouldHaveSize(3)
                    parameters[0].apply {
                        name shouldBe "environment"
                        val v0 = value.shouldBeInstanceOf<Annotation.Value.Single>()
                        v0.value shouldBe "dev"
                    }
                    parameters[1].apply {
                        name shouldBe "tags"
                        val arr = value.shouldBeInstanceOf<Annotation.Value.Array>()
                        arr.value.map { it.value } shouldBe listOf("tag1", "tag2")
                    }
                    parameters[2].apply {
                        name shouldBe "debug"
                        val v2 = value.shouldBeInstanceOf<Annotation.Value.Single>()
                        v2.value shouldBe "true"
                    }
                }
            }
    }

    @Test
    fun testAnnotationWithDictParameter() {
        val source =
            // language=ws
            """
            |@Test(
            |    list: ["Test"],
            |    dict: {test: "hello"}
            |)
            |type Hello {
            |    world: Integer
            |}
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Type>()
            .apply {
                identifier.value shouldBe "Hello"
                annotations.shouldHaveSize(1)
                annotations.first().apply {
                    name shouldBe "Test"
                    parameters.shouldHaveSize(2)
                    parameters[0].apply {
                        name shouldBe "list"
                        val arr = value.shouldBeInstanceOf<Annotation.Value.Array>()
                        arr.value.map { it.value } shouldBe listOf("Test")
                    }
                    parameters[1].apply {
                        name shouldBe "dict"
                        val dict = value.shouldBeInstanceOf<Annotation.Value.Dict>()
                        dict.value.shouldHaveSize(1)
                        dict.value[0].apply {
                            name shouldBe "test"
                            val inner = value.shouldBeInstanceOf<Annotation.Value.Single>()
                            inner.value shouldBe "hello"
                        }
                    }
                }
            }
    }

    @Test
    fun testSimpleAnnotationOnField() {
        val source =
            // language=ws
            """
            |type User {
            |  @Deprecated
            |  name: String
            |}
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Type>()
            .apply {
                identifier.value shouldBe "User"
                shape.value.shouldHaveSize(1).first().apply {
                    identifier.value shouldBe "name"
                    annotations.shouldHaveSize(1).first().name shouldBe "Deprecated"
                }
            }
    }

    @Test
    fun testMultipleAnnotationsOnField() {
        val source =
            // language=ws
            """
            |type User {
            |  @Deprecated
            |  @Internal
            |  name: String,
            |  age: Integer
            |}
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Type>()
            .apply {
                identifier.value shouldBe "User"
                shape.value.shouldHaveSize(2).toList().let { (fieldA, fieldB) ->
                    fieldA.apply {
                        identifier.value shouldBe "name"
                        annotations.shouldHaveSize(2).toList().let { (a, b) ->
                            a.name shouldBe "Deprecated"
                            b.name shouldBe "Internal"
                        }
                    }
                    fieldB.apply {
                        identifier.value shouldBe "age"
                        annotations.shouldHaveSize(0)
                    }
                }
            }
    }

    @Test
    fun testAnnotationWithParametersOnField() {
        val source =
            // language=ws
            """
            |type User {
            |  @Validate(min: 1, max: 100)
            |  name: String
            |}
            """.trimMargin()

        parser(source)
            .shouldBeRightOrReport()
            .shouldHaveSize(1)
            .first()
            .shouldBeInstanceOf<Type>()
            .apply {
                identifier.value shouldBe "User"
                shape.value.shouldHaveSize(1)
                    .first().apply {
                        identifier.value shouldBe "name"
                        annotations.shouldHaveSize(1)
                            .first().apply {
                                name shouldBe "Validate"
                                parameters.shouldHaveSize(2).toList().let { (a, b) ->
                                    a.apply {
                                        name shouldBe "min"
                                        value shouldBe Annotation.Value.Single("1")
                                    }
                                    b.apply {
                                        name shouldBe "max"
                                        value shouldBe Annotation.Value.Single("100")
                                    }
                                }
                            }
                    }
            }
    }

    @Test
    fun testEndpointResponseAnnotationParser() {
        val source =
            // language=ws
            """
            |type MyResponse {
            |    id: String
            |}
            |endpoint MyEndpoint GET /path -> {
            |    @Status("created")
            |    201 -> MyResponse
            |}
            """.trimMargin()

        val result = parser(source)

        result
            .shouldBeRightOrReport()
            .shouldHaveSize(2)
            .last()
            .shouldBeInstanceOf<Endpoint>()
            .apply {
                responses.shouldHaveSize(1).first().apply {
                    status shouldBe "201"
                    annotations.shouldHaveSize(1).first().apply {
                        name shouldBe "Status"
                        parameters.shouldHaveSize(1).first().value shouldBe Annotation.Value.Single("created")
                    }
                }
            }
    }
}

private fun Either<NonEmptyList<WirespecException>, NonEmptyList<Definition>>.shouldBeRightOrReport() = shouldBeRight { exceptions -> exceptions.joinToString { it.message } }
