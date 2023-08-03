package community.flock.wirespec.openapi.v2

import community.flock.kotlinx.openapi.bindings.v2.OpenAPI
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Identifier
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Custom
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.openapi.IO
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class OpenApiParserTest {

    @Test
    fun petstore() {
        val json = IO.readOpenApi("v2/petstore.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        val expectedDefinitions = listOf(
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
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
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
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
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
        val definition = ast.filterIsInstance<Type>()
        assertEquals(expectedDefinitions, definition)

        val endpoints = ast.filterIsInstance<Endpoint>().map { it.name }
        assertEquals(listOf("UploadFile", "AddPet", "AddPet", "UpdatePet", "UpdatePet", "UpdatePetWithForm", "PlaceOrder", "CreateUsersWithArrayInput", "CreateUsersWithListInput", "UpdateUser", "CreateUser"), endpoints)

        println(ast)
    }

    @Test
    fun objectInRequest() {
        val json = IO.readOpenApi("v2/object-in-request.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        val expectedDefinitions = listOf(
            Endpoint(
                name = "Test",
                method = Endpoint.Method.GET,
                path = listOf(Endpoint.Segment.Literal(value = "test")),
                query = emptyList(),
                headers = emptyList(),
                cookies = emptyList(),
                requests = listOf(
                    Endpoint.Request(
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Custom(value = "TestRequestBody", isIterable = false),
                            isNullable = false
                        )
                    )
                ),
                responses = listOf(
                    Endpoint.Response(
                        status = "200",
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = false
                        )
                    )
                )
            ),
            Type(
                name = "TestRequestBody",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "id"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "nest"),
                            reference = Custom(value = "TestRequestBodyNest", isIterable = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Type(
                name = "TestRequestBodyNest",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "a"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "b"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        )
                    )
                )
            )
        )

        assertEquals(expectedDefinitions, ast)

        println(ast)
    }

    @Test
    fun objectInResponse() {
        val json = IO.readOpenApi("v2/object-in-response.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        val expectedDefinitions = listOf(
            Endpoint(
                name = "Test",
                method = Endpoint.Method.GET,
                path = listOf(Endpoint.Segment.Literal(value = "test")),
                query = emptyList(),
                headers = emptyList(),
                cookies = emptyList(),
                requests = listOf(
                    Endpoint.Request(
                        content = null
                    )
                ),
                responses = listOf(
                    Endpoint.Response(
                        status = "200",
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Custom(value = "Test200ResponseBody", isIterable = false),
                            isNullable = false
                        )
                    )
                )
            ),
            Type(
                name = "Test200ResponseBody",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "id"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "nest"),
                            reference = Custom(value = "Test200ResponseBodyNest", isIterable = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Type(
                name = "Test200ResponseBodyNest",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "a"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "b"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        )
                    )
                )
            )
        )

        assertEquals(expectedDefinitions, ast)

        println(ast)
    }

    @Test
    fun additionalproperties() {
        val json = IO.readOpenApi("v2/additionalproperties.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        val expectedDefinitions = listOf(
            Endpoint(
                name = "AdditionalProperties",
                method = Endpoint.Method.GET,
                path = listOf(Endpoint.Segment.Literal(value = "additional"), Endpoint.Segment.Literal(value = "properties")),
                query = emptyList(),
                headers = emptyList(),
                cookies = emptyList(),
                requests = listOf(
                    Endpoint.Request(
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Field.Reference.Custom(value = "Message", isIterable = false, isMap = true),
                            isNullable = false
                        )
                    )
                ),
                responses = listOf(
                    Endpoint.Response(
                        status = "200",
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Field.Reference.Custom(value = "Message", isIterable = false, isMap = true),
                            isNullable = false
                        )
                    ),
                    Endpoint.Response(
                        status = "404",
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Field.Reference.Custom(value = "AdditionalProperties404ResponseBody", isIterable = false, isMap = true),
                            isNullable = false
                        )
                    )
                )
            ),
            Type(
                name="AdditionalProperties404ResponseBody",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "code"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "text"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Type(
                name = "Message",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "code"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier(value = "text"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false),
                            isNullable = true
                        )
                    )
                )
            )
        )

        assertEquals(expectedDefinitions, ast)

        println(ast)
    }
}
