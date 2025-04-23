package community.flock.wirespec.openapi.v2

import community.flock.kotlinx.openapi.bindings.v2.OpenAPI
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Reference.Custom
import community.flock.wirespec.compiler.core.parse.Reference.Iterable
import community.flock.wirespec.compiler.core.parse.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.openapi.common.Ast
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser.parse
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenAPIV2ParserTest {

    @Test
    fun query() {
        val path = Path("src/commonTest/resources/v2/query.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        val endpoint = ast
            .filterIsInstance<Endpoint>()
            .find { it.identifier.value == "QueryGET" }

        val fields = endpoint?.queries?.find { it.identifier.value == "order" }

        val expected = Iterable(
            reference = Primitive(
                type = Primitive.Type.String,
                isNullable = false,
            ),
            isNullable = true,
        )
        assertEquals(expected, fields?.reference)
    }

    @Test
    fun petstore() {
        val path = Path("src/commonTest/resources/v2/petstore.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        val expectedTypeDefinitions = listOf(
            Type(
                comment = null,
                identifier = DefinitionIdentifier("ApiResponse"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("code"),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P32),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("type"),
                            reference = Primitive(type = Primitive.Type.String, isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("message"),
                            reference = Primitive(type = Primitive.Type.String, isNullable = true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("Category"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Primitive(type = Primitive.Type.Integer(), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            reference = Primitive(type = Primitive.Type.String, isNullable = true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("Pet"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Primitive(type = Primitive.Type.Integer(), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("category"),
                            reference = Custom(value = "Category", isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            reference = Primitive(type = Primitive.Type.String, isNullable = false),
                        ),
                        Field(
                            identifier = FieldIdentifier("photoUrls"),
                            reference = Iterable(
                                reference = Primitive(
                                    type = Primitive.Type.String,
                                    isNullable = false,
                                ),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("tags"),
                            reference = Iterable(
                                reference = Custom(value = "Tag", isNullable = false),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("status"),
                            reference = Custom(value = "PetStatus", isNullable = true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("Tag"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Primitive(type = Primitive.Type.Integer(), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            reference = Primitive(type = Primitive.Type.String, isNullable = true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("Order"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Primitive(type = Primitive.Type.Integer(), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("petId"),
                            reference = Primitive(type = Primitive.Type.Integer(), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("quantity"),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P32),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("shipDate"),
                            reference = Primitive(type = Primitive.Type.String, isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("status"),
                            reference = Custom(value = "OrderStatus", isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("complete"),
                            reference = Primitive(type = Primitive.Type.Boolean, isNullable = true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("User"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Primitive(type = Primitive.Type.Integer(), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("username"),
                            reference = Primitive(type = Primitive.Type.String, isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("firstName"),
                            reference = Primitive(type = Primitive.Type.String, isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("lastName"),
                            reference = Primitive(type = Primitive.Type.String, isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("email"),
                            reference = Primitive(type = Primitive.Type.String, isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("password"),
                            reference = Primitive(type = Primitive.Type.String, isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("phone"),
                            reference = Primitive(type = Primitive.Type.String, isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("userStatus"),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P32),
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
                identifier = DefinitionIdentifier("PetStatus"),
                entries = setOf("available", "pending", "sold"),
            ),
            Enum(
                comment = null,
                identifier = DefinitionIdentifier("OrderStatus"),
                entries = setOf("placed", "approved", "delivered"),
            ),
        )

        val typeDefinitions: List<Type> = ast.filterIsInstance<Type>()
        assertEquals(expectedTypeDefinitions, typeDefinitions)

        val enumDefinitions: List<Enum> = ast.filterIsInstance<Enum>()
        assertEquals(enumDefinitions, expectedEnumDefinitions)

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
        assertEquals(expectedEndpoint, endpoints)
    }

    @Test
    fun alias() {
        val path = Path("src/commonTest/resources/v2/alias.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        val expected = listOf(
            Endpoint(
                comment = null,
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
                    ),
                ),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("Foo"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("a"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
        )
        assertEquals(expected, ast)
    }

    @Test
    fun objectInRequest() {
        val path = Path("src/commonTest/resources/v2/object-in-request.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Ast.objectInRequest, ast)
    }

    @Test
    fun objectInResponse() {
        val path = Path("src/commonTest/resources/v2/object-in-response.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Ast.objectInResponse, ast)
    }

    @Test
    fun additionalProperties() {
        val path = Path("src/commonTest/resources/v2/additionalproperties.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Ast.additionalProperties, ast)
    }

    @Test
    fun array() {
        val path = Path("src/commonTest/resources/v2/array.json")
        val json = SystemFileSystem.source(path).buffered().readString()
        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Ast.array, ast)
    }

    @Test
    fun allOf() {
        val path = Path("src/commonTest/resources/v2/allof.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Ast.allOf, ast)
    }

    @Test
    fun enum() {
        val path = Path("src/commonTest/resources/v2/enum.json")
        val json = SystemFileSystem.source(path).buffered().readString()
        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Ast.enum, ast)
    }
}
