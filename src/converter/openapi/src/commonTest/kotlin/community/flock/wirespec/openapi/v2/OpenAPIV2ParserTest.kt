package community.flock.wirespec.openapi.v2

import arrow.core.nonEmptyListOf
import community.flock.kotlinx.openapi.bindings.OpenAPIV2
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Reference.Custom
import community.flock.wirespec.compiler.core.parse.ast.Reference.Iterable
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Type.Shape
import community.flock.wirespec.openapi.common.Ast
import community.flock.wirespec.openapi.common.toDescriptionAnnotationList
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser.parse
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test

class OpenAPIV2ParserTest {

    @Test
    fun query() {
        val path = Path("src/commonTest/resources/v2/query.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV2.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        val endpoint = ast
            .filterIsInstance<Endpoint>()
            .find { it.identifier.value == "QueryGET" }

        val fields = endpoint?.queries?.find { it.identifier.value == "order" }

        val expected = Iterable(
            reference = Primitive(
                type = Primitive.Type.String(null),
                isNullable = false,
            ),
            isNullable = true,
        )
        fields?.reference shouldBe expected
    }

    @Test
    fun petstore() {
        val path = Path("src/commonTest/resources/v2/petstore.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV2.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        val expectedTypeDefinitions = listOf(
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("ApiResponse"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("code"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P32, null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("type"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.String(null), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("message"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.String(null), isNullable = true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Category"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.Integer(constraint = null), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.String(null), isNullable = true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Pet"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.Integer(constraint = null), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("category"),
                            annotations = emptyList(),
                            reference = Custom(value = "Category", isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.String(null), isNullable = false),
                        ),
                        Field(
                            identifier = FieldIdentifier("photoUrls"),
                            annotations = emptyList(),
                            reference = Iterable(
                                reference = Primitive(
                                    type = Primitive.Type.String(null),
                                    isNullable = false,
                                ),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("tags"),
                            annotations = emptyList(),
                            reference = Iterable(
                                reference = Custom(value = "Tag", isNullable = false),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("status"),
                            annotations = emptyList(),
                            reference = Custom(value = "PetStatus", isNullable = true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Tag"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.Integer(constraint = null), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.String(null), isNullable = true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Order"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.Integer(constraint = null), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("petId"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.Integer(constraint = null), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("quantity"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P32, null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("shipDate"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(
                                    constraint = null,
                                ),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("status"),
                            annotations = emptyList(),
                            reference = Custom(value = "OrderStatus", isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("complete"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.Boolean, isNullable = true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("User"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.Integer(constraint = null), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("username"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.String(null), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("firstName"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.String(null), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("lastName"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.String(null), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("email"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.String(null), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("password"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.String(null), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("phone"),
                            annotations = emptyList(),
                            reference = Primitive(type = Primitive.Type.String(null), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("userStatus"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P32, null),
                                isNullable = true,
                            ),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
        )

        val expectedEnumDefinitions = listOf(
            Enum(
                comment = null,
                annotations = "pet status in the store".toDescriptionAnnotationList(),
                identifier = DefinitionIdentifier("PetStatus"),
                entries = setOf("available", "pending", "sold"),
            ),
            Enum(
                comment = null,
                annotations = "Order Status".toDescriptionAnnotationList(),
                identifier = DefinitionIdentifier("OrderStatus"),
                entries = setOf("placed", "approved", "delivered"),
            ),
        )

        val typeDefinitions: List<Type> = ast.filterIsInstance<Type>()
        typeDefinitions shouldBe expectedTypeDefinitions

        val enumDefinitions: List<Enum> = ast.filterIsInstance<Enum>()
        expectedEnumDefinitions shouldBe enumDefinitions

        val endpoints = ast.filterIsInstance<Endpoint>().map { it.identifier.value }
        val expectedEndpoint = listOf(
            "UploadFile",
            "AddPet",
            "UpdatePet",
            "FindPetsByStatus",
            "FindPetsByTags",
            "GetPetById",
            "UpdatePetWithForm",
            "DeletePet",
            "PlaceOrder",
            "GetOrderById",
            "DeleteOrder",
            "CreateUsersWithArrayInput",
            "CreateUsersWithListInput",
            "GetUserByName",
            "UpdateUser",
            "DeleteUser",
            "LoginUser",
            "LogoutUser",
            "CreateUser",
        )
        endpoints shouldBe expectedEndpoint
    }

    @Test
    fun alias() {
        val path = Path("src/commonTest/resources/v2/alias.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV2.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        val expected = listOf(
            Endpoint(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("AlisaGET"),
                method = Endpoint.Method.GET,
                path = listOf(Endpoint.Segment.Literal(value = "alisa")),
                queries = emptyList(),
                headers = emptyList(),
                requests = listOf(Endpoint.Request(content = null)),
                responses = listOf(
                    Endpoint.Response(
                        status = "200",
                        headers = emptyList(),
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Custom(value = "Foo", isNullable = false),
                        ),
                        annotations = "Ok".toDescriptionAnnotationList(),
                    ),
                ),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Foo"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("a"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = true,
                            ),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
        )
        ast shouldBe expected
    }

    @Test
    fun objectInRequest() {
        val path = Path("src/commonTest/resources/v2/object-in-request.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV2.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        ast shouldBe Ast.objectInRequest
    }

    @Test
    fun objectInResponse() {
        val path = Path("src/commonTest/resources/v2/object-in-response.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV2.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        ast shouldBe Ast.objectInResponse
    }

    @Test
    fun additionalProperties() {
        val path = Path("src/commonTest/resources/v2/additionalproperties.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV2.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        ast shouldBe Ast.additionalProperties
    }

    @Test
    fun array() {
        val path = Path("src/commonTest/resources/v2/array.json")
        val json = SystemFileSystem.source(path).buffered().readString()
        val openApi = OpenAPIV2.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        ast shouldBe Ast.array
    }

    @Test
    fun allOf() {
        val path = Path("src/commonTest/resources/v2/allof.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV2.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        ast shouldBe Ast.allOf
    }

    @Test
    fun enum() {
        val path = Path("src/commonTest/resources/v2/enum.json")
        val json = SystemFileSystem.source(path).buffered().readString()
        val openApi = OpenAPIV2.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        ast shouldBe Ast.enum
    }

    @Test
    fun emptyResponses() {
        val path = Path("src/commonTest/resources/v2/empty-response.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV2.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        val expected = nonEmptyListOf(
            Endpoint(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name = "EmptyGET"),
                method = Endpoint.Method.GET,
                path = listOf(
                    Endpoint.Segment.Literal(value = "empty"),
                ),
                queries = emptyList(),
                headers = emptyList(),
                requests = listOf(
                    Endpoint.Request(content = null),
                ),
                responses = listOf(
                    Endpoint.Response(
                        status = "200",
                        headers = emptyList(),
                        content = null,
                        annotations = "Ok".toDescriptionAnnotationList(),
                    ),
                ),
            ),
        )
        ast shouldBe expected
    }

    @Test
    fun testDescriptionParsing() {
        val json =
            // language=json
            """
            |{
            |    "swagger": "2.0",
            |    "info": {
            |        "title": "Test API",
            |        "version": "1.0.0"
            |    },
            |    "definitions": {
            |        "Todo": {
            |            "description": "Todo object",
            |            "properties": {
            |                "id": {
            |                    "type": "string",
            |                    "description": "id field"
            |                }
            |            }
            |        }
            |    },
            |    "paths": {
            |        "/todos": {
            |            "get": {
            |                "description": "Get all todos",
            |                "responses": {
            |                    "200": {
            |                        "description": "Successful response"
            |                    }
            |                }
            |            }
            |        }
            |    }
            |}
            """.trimMargin()

        val ast = OpenAPIV2Parser.parse(ModuleContent(FileUri("test.json"), json), false)
        val definitions = ast.modules.head.statements

        val todo = definitions.find { (it as? Type)?.identifier?.value == "Todo" }.shouldBeInstanceOf<Type>()
        todo.annotations shouldContain Annotation(
            "Description",
            listOf(Annotation.Parameter("default", Annotation.Value.Single("Todo object"))),
        )

        val endpoint =
            definitions.find { (it as? Endpoint)?.identifier?.value == "TodosGET" }.shouldBeInstanceOf<Endpoint>()
        endpoint.annotations shouldContain Annotation(
            "Description",
            listOf(Annotation.Parameter("default", Annotation.Value.Single("Get all todos"))),
        )
    }
}
