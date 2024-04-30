package community.flock.wirespec.openapi.v3

import community.flock.kotlinx.openapi.bindings.v3.OpenAPI
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Identifier
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.openapi.IO
import community.flock.wirespec.openapi.common.Expected
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiParserTest {

    @Test
    fun petstore() {
        val json = IO.readFile("v3/petstore.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        val expect = listOf(
            Enum(name = "FindPetsByStatusParameterStatus", entries = setOf("available", "pending", "sold")),
            Type(
                name = "Order",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier("id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("petId"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("quantity"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("shipDate"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("status"),
                            reference = Reference.Custom(value = "OrderStatus", isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("complete"),
                            reference = Primitive(type = Primitive.Type.Boolean, isIterable = false, isMap = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Enum(name = "OrderStatus", entries = setOf("placed", "approved", "delivered")),
            Type(
                name = "Customer",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier("id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("username"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("address"),
                            reference = Reference.Custom(value = "Address", isIterable = true, isMap = false),
                            isNullable = true
                        )
                    )
                )
            ),
            Type(
                name = "Address",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier("street"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("city"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("state"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("zip"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false, isMap = false),
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
                            identifier = Identifier("id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("name"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false, isMap = false),
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
                            identifier = Identifier("id"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("username"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("firstName"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("lastName"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("email"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("password"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("phone"),
                            reference = Primitive(type = Primitive.Type.String, isIterable = false, isMap = false),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("userStatus"),
                            reference = Primitive(type = Primitive.Type.Integer, isIterable = false, isMap = false),
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
                            identifier = Identifier("id"),
                            reference = Primitive(
                                type = Primitive.Type.Integer,
                                isIterable = false,
                                isMap = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("name"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isMap = false
                            ),
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
                            identifier = Identifier("id"),
                            reference = Primitive(
                                type = Primitive.Type.Integer,
                                isIterable = false,
                                isMap = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("name"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isMap = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier("category"),
                            reference = Reference.Custom(
                                value = "Category",
                                isIterable = false,
                                isMap = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("photoUrls"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = true,
                                isMap = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier("tags"),
                            reference = Reference.Custom(
                                value = "Tag",
                                isIterable = true,
                                isMap = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("status"),
                            reference = Reference.Custom(
                                value = "PetStatus",
                                isIterable = false,
                                isMap = false
                            ),
                            isNullable = true
                        )
                    )
                )
            ),
            Enum(name = "PetStatus", entries = setOf("available", "pending", "sold")),
            Type(
                name = "ApiResponse",
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier("code"),
                            reference = Primitive(
                                type = Primitive.Type.Integer,
                                isIterable = false,
                                isMap = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("type"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isMap = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = Identifier("message"),
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

        assertEquals(expect.filterIsInstance<Type>(), ast.filterIsInstance<Type>())
        assertEquals(expect.filterIsInstance<Enum>(), ast.filterIsInstance<Enum>())

        val endpoint = ast.filterIsInstance<Endpoint>().find { it.name == "GetInventory" }

        val expectedEndpoint = Endpoint(
            name = "GetInventory",
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "store"), Endpoint.Segment.Literal(value = "inventory")),
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
                        reference = Primitive(type = Primitive.Type.Integer, isIterable = false, isMap = true),
                        isNullable = false
                    )
                )
            )
        )
        assertEquals(expectedEndpoint, endpoint)
    }

    @Test
    fun pizza() {
        val json = IO.readFile("v3/pizza.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        val expect = listOf(
            Endpoint(
                "PizzasPizzaIdIngredientsGET",
                Endpoint.Method.GET,
                listOf(
                    Endpoint.Segment.Literal("pizzas"),
                    Endpoint.Segment.Param(
                        Identifier("pizzaId"),
                        Primitive(type = Primitive.Type.String, isIterable = false)
                    ),
                    Endpoint.Segment.Literal("ingredients"),
                ),
                listOf(),
                listOf(),
                listOf(),
                listOf(
                    Endpoint.Request(null),
                ),
                listOf(
                    Endpoint.Response(
                        status = "200",
                        headers = emptyList(),
                        content = Endpoint.Content("application/json", Reference.Custom("Ingredient", true), false)
                    ),
                    Endpoint.Response(
                        status = "404",
                        headers = emptyList(),
                        content = null
                    )
                )
            ),
            Type(
                "Ingredient",
                Shape(
                    listOf(
                        Field(Identifier("id"), Primitive(Primitive.Type.String, false), true),
                        Field(Identifier("name"), Primitive(Primitive.Type.String, false), true),
                        Field(Identifier("quantity"), Primitive(Primitive.Type.String, false), true),
                    )
                )
            ),
        )
        assertEquals(expect, ast)
    }

    @Test
    fun todo() {

        val json = IO.readFile("v3/todo.json")

        val openApi = OpenAPI.decodeFromString(json)

        val ast = OpenApiParser.parse(openApi)

        val expect = listOf(
            Endpoint(
                "TodosList",
                Endpoint.Method.GET,
                listOf(
                    Endpoint.Segment.Literal("todos")
                ),
                listOf(
                    Field(Identifier("completed"), Primitive(type = Primitive.Type.Boolean, isIterable = false), true)
                ),
                listOf(
                    Field(Identifier("x-user"), Primitive(type = Primitive.Type.Boolean, isIterable = false), true)
                ),
                listOf(),
                listOf(
                    Endpoint.Request(null),
                ),
                listOf(
                    Endpoint.Response(
                        status = "200",
                        headers = emptyList(),
                        content = Endpoint.Content("application/json", Reference.Custom("Todo", true), false)
                    ),
                    Endpoint.Response(
                        status = "500",
                        headers = emptyList(),
                        content = Endpoint.Content("application/json", Reference.Custom("Error", false), false)
                    )
                )
            ),
            Endpoint(
                "TodosPOST",
                Endpoint.Method.POST,
                listOf(
                    Endpoint.Segment.Literal("todos"),
                ),
                listOf(),
                listOf(
                    Field(Identifier("x-user"), Primitive(type = Primitive.Type.Boolean, isIterable = false), true)
                ),
                listOf(),
                listOf(
                    Endpoint.Request(
                        Endpoint.Content(
                            type = "application/json",
                            reference = Reference.Custom("Todo_input", false),
                            isNullable = false
                        )
                    ),
                    Endpoint.Request(
                        Endpoint.Content(
                            type = "application/xml",
                            reference = Reference.Custom("Todo", false),
                            isNullable = false
                        )
                    )
                ),
                listOf(
                    Endpoint.Response(
                        status = "201",
                        headers = emptyList(),
                        content = null
                    ),
                    Endpoint.Response(
                        status = "500",
                        headers = emptyList(),
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Reference.Custom("Error", false),
                            isNullable = false
                        )
                    )
                )
            ),
            Endpoint(
                "TodosIdGET",
                Endpoint.Method.GET,
                listOf(
                    Endpoint.Segment.Literal("todos"),
                    Endpoint.Segment.Param(Identifier("id"), Primitive(Primitive.Type.String, false))
                ),
                listOf(),
                listOf(),
                listOf(),
                listOf(
                    Endpoint.Request(null),
                ),
                listOf(
                    Endpoint.Response(
                        status = "200",
                        headers = emptyList(),
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Reference.Custom("Todo", false),
                            isNullable = true
                        )
                    ),
                    Endpoint.Response(
                        status = "500",
                        headers = emptyList(),
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Reference.Custom("Error", false),
                            isNullable = false
                        )
                    )
                )
            ),
            Type(
                "Todo_input",
                Shape(
                    listOf(
                        Field(Identifier("title"), Primitive(Primitive.Type.String, false), true),
                        Field(Identifier("completed"), Primitive(Primitive.Type.Boolean, false), true)
                    )
                )
            ),
            Type(
                "Todo",
                Shape(
                    listOf(
                        Field(Identifier("id"), Primitive(Primitive.Type.String, false), true),
                        Field(Identifier("title"), Primitive(Primitive.Type.String, false), true),
                        Field(Identifier("completed"), Primitive(Primitive.Type.Boolean, false), true),
                        Field(Identifier("alert"), Reference.Custom("TodoAlert", false), true),
                    )
                )
            ),
            Type(
                "TodoAlert",
                Shape(
                    listOf(
                        Field(Identifier("code"), Primitive(Primitive.Type.String, false), true),
                        Field(Identifier("message"), Reference.Custom("TodoAlertMessage", false), true),
                    )
                )
            ),
            Type(
                "TodoAlertMessage",
                Shape(
                    listOf(
                        Field(Identifier("key"), Primitive(Primitive.Type.String, false), true),
                        Field(Identifier("value"), Primitive(Primitive.Type.String, false), true),
                    )
                )
            ),
            Type(
                "TodosnestedArray",
                Shape(
                    listOf(
                        Field(Identifier("id"), Primitive(Primitive.Type.String, false), true),
                        Field(Identifier("title"), Primitive(Primitive.Type.String, false), true),
                        Field(Identifier("nested"), Primitive(Primitive.Type.Boolean, false), true),
                    )
                )
            ),
            Type(
                "Error",
                Shape(
                    listOf(
                        Field(Identifier("code"), Primitive(Primitive.Type.String, false), true),
                        Field(Identifier("message"), Primitive(Primitive.Type.String, false), true),
                    )
                )
            ),
        )

        assertEquals(expect, ast)
    }

    @Test
    fun objectInRequest() {
        val json = IO.readFile("v3/object-in-request.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        assertEquals(Expected.objectInRequest, ast)

        println(ast)
    }

    @Test
    fun objectInResponse() {
        val json = IO.readFile("v3/object-in-response.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        assertEquals(Expected.objectInResponse, ast)

        println(ast)
    }

    @Test
    fun additionalProperties() {
        val json = IO.readFile("v3/additionalproperties.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        assertEquals(Expected.additionalproperties, ast)

        println(ast)
    }

    @Test
    fun array() {
        val json = IO.readFile("v3/array.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        assertEquals(Expected.array, ast)

        println(ast)
    }

    @Test
    fun allOf() {
        val json = IO.readFile("v3/allof.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        assertEquals(Expected.allOf, ast)
    }

    @Test
    fun oneOf() {
        val json = IO.readFile("v3/oneof.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        Expected.oneOf.zip(ast).forEach { (expected, actual) ->
            println(expected.name)
            assertEquals(expected, actual)
        }

    }

    @Test
    fun enum() {
        val json = IO.readFile("v3/enum.json")

        val openApi = OpenAPI.decodeFromString(json)
        val ast = OpenApiParser.parse(openApi)

        assertEquals(Expected.enum, ast)
    }
}
