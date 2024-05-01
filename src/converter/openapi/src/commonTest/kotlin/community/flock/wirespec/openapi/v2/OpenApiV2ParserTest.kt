package community.flock.wirespec.openapi.v2

import com.goncalossilva.resources.Resource
import community.flock.kotlinx.openapi.bindings.v2.OpenAPI
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Field.Reference.Custom
import community.flock.wirespec.compiler.core.parse.Field.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.openapi.common.Expected
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiV2ParserTest {

    @Test
    fun petstore() {
        val json = Resource("src/commonTest/resources/v2/petstore.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiV2Parser.parse(openApi)

        val expectedTypeDefinitions = listOf(
            Type(
                identifier = Identifier("ApiResponse"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier("code"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("type"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("message"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Type(
                identifier = Identifier("Category"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier("id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("name"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Type(
                identifier = Identifier("Pet"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier("id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("category"),
                            reference = Custom(value = "Category", isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("name"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier("photoUrls"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = true),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier("tags"),
                            reference = Custom(value = "Tag", isIterable = true),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("status"),
                            reference = Custom(value = "PetStatus", isIterable = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Type(
                identifier = Identifier("Tag"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier("id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("name"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Type(
                identifier = Identifier("Order"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier("id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("petId"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("quantity"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("shipDate"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("status"),
                            reference = Custom(value = "OrderStatus", isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("complete"),
                            reference = Primitive(type = Primitive.Type.Boolean, isIterable = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Type(
                identifier = Identifier("User"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier("id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("username"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("firstName"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("lastName"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("email"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("password"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("phone"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("userStatus"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        )
                    )
                )
            )
        )

        val expectedEnumDefinitions = listOf(
            Enum(identifier = Identifier("PetStatus"), entries = setOf("available", "pending", "sold")),
            Enum(identifier = Identifier("OrderStatus"), entries = setOf("placed", "approved", "delivered"))
        )

        val typeDefinitions = ast.filterIsInstance<Type>()
        assertEquals(expectedTypeDefinitions, typeDefinitions)

        val enumDefinitions = ast.filterIsInstance<Enum>()
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
            "CreateUser"
        )
        assertEquals(expectedEndpoint, endpoints)
    }

    @Test
    fun alias() {
        val json = Resource("src/commonTest/resources/v2/alias.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiV2Parser.parse(openApi)

        val expected = listOf(
            Endpoint(
                identifier = Identifier("AlisaGET"),
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
                identifier = Identifier("Foo"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier("a"),
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
        val json = Resource("src/commonTest/resources/v2/object-in-request.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiV2Parser.parse(openApi)

        assertEquals(Expected.objectInRequest, ast)
    }

    @Test
    fun objectInResponse() {
        val json = Resource("src/commonTest/resources/v2/object-in-response.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiV2Parser.parse(openApi)

        assertEquals(Expected.objectInResponse, ast)
    }

    @Test
    fun additionalProperties() {
        val json = Resource("src/commonTest/resources/v2/additionalproperties.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiV2Parser.parse(openApi)

        assertEquals(Expected.additionalproperties, ast)
    }

    @Test
    fun array() {
        val json = Resource("src/commonTest/resources/v2/array.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiV2Parser.parse(openApi)

        assertEquals(Expected.array, ast)
    }

    @Test
    fun allOf() {
        val json = Resource("src/commonTest/resources/v2/allof.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiV2Parser.parse(openApi)

        assertEquals(Expected.allOf, ast)
    }

    @Test
    fun enum() {
        val json = Resource("src/commonTest/resources/v2/enum.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiV2Parser.parse(openApi)

        assertEquals(Expected.enum, ast)
    }
}
