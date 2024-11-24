package community.flock.wirespec.openapi.v2

import com.goncalossilva.resources.Resource
import community.flock.kotlinx.openapi.bindings.v2.OpenAPI
import community.flock.wirespec.compiler.core.parse.*
import community.flock.wirespec.compiler.core.parse.Reference.Custom
import community.flock.wirespec.compiler.core.parse.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.openapi.common.Expected
import community.flock.wirespec.openapi.v2.OpenApiV2Parser.parse
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiV2ParserTest {

    @Test
    fun petstore() {
        val json = Resource("src/commonTest/resources/v2/petstore.json").readText()

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
                            reference = Primitive(type = Primitive.Type.Integer(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("type"),
                            reference = Primitive(type = Primitive.Type.String(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("message"),
                            reference = Primitive(type = Primitive.Type.String(), isIterable = false),
                            isNullable = true
                        )
                    )
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
                            reference = Primitive(type = Primitive.Type.Integer(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            reference = Primitive(type = Primitive.Type.String(), isIterable = false),
                            isNullable = true
                        )
                    )
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
                            reference = Primitive(type = Primitive.Type.Integer(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("category"),
                            reference = Custom(value = "Category", isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            reference = Primitive(type = Primitive.Type.String(), isIterable = false),
                            isNullable = false
                        ),
                        Field(
                            identifier = FieldIdentifier("photoUrls"),
                            reference = Primitive(type = Primitive.Type.String(), isIterable = true),
                            isNullable = false
                        ),
                        Field(
                            identifier = FieldIdentifier("tags"),
                            reference = Custom(value = "Tag", isIterable = true),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("status"),
                            reference = Custom(value = "PetStatus", isIterable = false),
                            isNullable = true
                        )
                    )
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
                            reference = Primitive(type = Primitive.Type.Integer(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            reference = Primitive(type = Primitive.Type.String(), isIterable = false),
                            isNullable = true
                        )
                    )
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
                            reference = Primitive(type = Primitive.Type.Integer(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("petId"),
                            reference = Primitive(type = Primitive.Type.Integer(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("quantity"),
                            reference = Primitive(type = Primitive.Type.Integer(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("shipDate"),
                            reference = Primitive(type = Primitive.Type.String(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("status"),
                            reference = Custom(value = "OrderStatus", isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("complete"),
                            reference = Primitive(type = Primitive.Type.Boolean(), isIterable = false),
                            isNullable = true
                        )
                    )
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
                            reference = Primitive(type = Primitive.Type.Integer(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("username"),
                            reference = Primitive(type = Primitive.Type.String(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("firstName"),
                            reference = Primitive(type = Primitive.Type.String(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("lastName"),
                            reference = Primitive(type = Primitive.Type.String(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("email"),
                            reference = Primitive(type = Primitive.Type.String(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("password"),
                            reference = Primitive(type = Primitive.Type.String(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("phone"),
                            reference = Primitive(type = Primitive.Type.String(), isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("userStatus"),
                            reference = Primitive(type = Primitive.Type.Integer(), isIterable = false),
                            isNullable = true
                        )
                    )
                ),
                extends = emptyList(),
            )
        )

        val expectedEnumDefinitions = listOf(
            Enum(
                comment = null,
                identifier = DefinitionIdentifier("PetStatus"),
                entries = setOf("available", "pending", "sold")
            ),
            Enum(
                comment = null,
                identifier = DefinitionIdentifier("OrderStatus"),
                entries = setOf("placed", "approved", "delivered")
            )
        )

//        val typeDefinitions:List<Type> = ast.filterIsInstance<Type>()
//        assertEquals(expectedTypeDefinitions, typeDefinitions)
//
//        val enumDefinitions:List<Enum> = ast.filterIsInstance<Enum>()
//        assertEquals(enumDefinitions, expectedEnumDefinitions)

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
        val ast = openApi.parse()

        val expected = listOf(
            Endpoint(
                comment = null,
                identifier = DefinitionIdentifier("AlisaGET"),
                method = Endpoint.Method.GET,
                path = listOf(Endpoint.Segment.Literal(value = "alisa")),
                queries = emptyList(),
                headers = emptyList(),
                cookies = emptyList(),
                requests = listOf(Endpoint.Request(content = null)),
                responses = listOf(
                    Endpoint.Response(
                        status = "200",
                        headers = emptyList(),
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Custom(value = "Foo", isIterable = false, isDictionary = false),
                            isNullable = false
                        )
                    )
                )
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("Foo"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("a"),
                            reference = Primitive(
                                type = Primitive.Type.String(),
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        )
                    )
                ),
                extends = emptyList(),
            )
        )
        assertEquals(expected, ast)
    }

    @Test
    fun objectInRequest() {
        val json = Resource("src/commonTest/resources/v2/object-in-request.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Expected.objectInRequest, ast)
    }

    @Test
    fun objectInResponse() {
        val json = Resource("src/commonTest/resources/v2/object-in-response.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Expected.objectInResponse, ast)
    }

    @Test
    fun additionalProperties() {
        val json = Resource("src/commonTest/resources/v2/additionalproperties.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Expected.additionalProperties, ast)
    }

    @Test
    fun array() {
        val json = Resource("src/commonTest/resources/v2/array.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Expected.array, ast)
    }

    @Test
    fun allOf() {
        val json = Resource("src/commonTest/resources/v2/allof.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Expected.allOf, ast)
    }

    @Test
    fun enum() {
        val json = Resource("src/commonTest/resources/v2/enum.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Expected.enum, ast)
    }
}
