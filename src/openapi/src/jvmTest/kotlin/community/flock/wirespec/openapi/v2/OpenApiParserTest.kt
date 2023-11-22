package community.flock.wirespec.openapi.v2

import community.flock.kotlinx.openapi.bindings.v2.OpenAPI
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Identifier
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Reference.Custom
import community.flock.wirespec.compiler.core.parse.nodes.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.openapi.IO
import community.flock.wirespec.openapi.common.Expected
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiParserTest {

    @Test
    fun petstore() {
        val json = IO.readOpenApi("v2/petstore.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        val expectedTypeDefinitions = listOf(
            Type(
                name = "ApiResponse",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "code"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "type"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "message"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Type(
                name = "Category",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "name"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Type(
                name = "Pet",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "category"),
                            reference = Custom(value = "Category", isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "name"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier(value = "photoUrls"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = true),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier(value = "tags"),
                            reference = Custom(value = "Tag", isIterable = true),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "status"),
                            reference = Custom(value = "PetStatus", isIterable = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Type(
                name = "Tag",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "name"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Type(
                name = "Order",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "petId"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "quantity"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "shipDate"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "status"),
                            reference = Custom(value = "OrderStatus", isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "complete"),
                            reference = Primitive(type = Primitive.Type.Boolean, isIterable = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Type(
                name = "User",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "username"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "firstName"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "lastName"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "email"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "password"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "phone"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "userStatus"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        )
                    )
                )
            )
        )

        val expectedEnumDefinitions = listOf(
            Enum(name="PetStatus", entries= setOf( "available", "pending", "sold")),
            Enum(name="OrderStatus", entries=setOf( "placed", "approved", "delivered"))
        )

        val typeDefinitions = ast.filterIsInstance<Type>()
        assertEquals(expectedTypeDefinitions, typeDefinitions)

        val enumDefinitions = ast.filterIsInstance<Enum>()
        assertEquals(enumDefinitions, expectedEnumDefinitions)

        val endpoints = ast.filterIsInstance<Endpoint>().map { it.name }
        val expectedEndpoint = listOf("UploadFile", "AddPet", "UpdatePet", "FindPetsByStatus", "FindPetsByTags", "GetPetById", "UpdatePetWithForm", "DeletePet", "PlaceOrder", "GetOrderById", "DeleteOrder", "CreateUsersWithArrayInput", "CreateUsersWithListInput", "GetUserByName", "UpdateUser", "DeleteUser", "LoginUser", "LogoutUser", "CreateUser")
        assertEquals(expectedEndpoint, endpoints)
    }

    @Test
    fun alias() {
        val json = IO.readOpenApi("v2/alias.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        val expected = listOf(
            Endpoint(
                name = "AlisaGET",
                method = Endpoint.Method.GET,
                path = listOf(Endpoint.Segment.Literal(value = "alisa")),
                query = emptyList(),
                headers = emptyList(),
                cookies = emptyList(),
                requests = listOf(Endpoint.Request(content = null)),
                responses = listOf(
                    Endpoint.Response(
                        status = "200",
                        headers = emptyList(),
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Custom(value = "Foo", isIterable = false, isMap = false),
                            isNullable = false
                        )
                    )
                )
            ),
            Type(
                name = "Foo",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "a"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isMap = false
                            ),
                            isNullable = true
                        )
                    )
                )
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun objectInRequest() {
        val json = IO.readOpenApi("v2/object-in-request.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        assertEquals(Expected.objectInRequest, ast)
    }

    @Test
    fun objectInResponse() {
        val json = IO.readOpenApi("v2/object-in-response.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        assertEquals(Expected.objectInResponse, ast)
    }

    @Test
    fun additionalProperties() {
        val json = IO.readOpenApi("v2/additionalproperties.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        assertEquals(Expected.additionalproperties, ast)
    }

    @Test
    fun array() {
        val json = IO.readOpenApi("v2/array.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        assertEquals(Expected.array, ast)
    }

    @Test
    fun allOf() {
        val json = IO.readOpenApi("v2/allof.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        assertEquals(Expected.allOf, ast)
    }

    @Test
    fun enum() {
        val json = IO.readOpenApi("v2/enum.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        assertEquals(Expected.enum, ast)
    }
}
