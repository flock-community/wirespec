package community.flock.wirespec.openapi.v3

import com.goncalossilva.resources.Resource
import community.flock.kotlinx.openapi.bindings.v3.OpenAPI
import community.flock.wirespec.compiler.core.parse.ClassIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.openapi.common.Expected
import community.flock.wirespec.openapi.v3.OpenApiV3Parser.parse
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiV3ParserTest {

    @Test
    fun petstore() {
        val json = Resource("src/commonTest/resources/v3/petstore.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        val expect = listOf(
            Enum(
                comment = null,
                identifier = ClassIdentifier("FindPetsByStatusParameterStatus"),
                entries = setOf("available", "pending", "sold")
            ),
            Type(
                comment = null,
                identifier = ClassIdentifier("Order"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Primitive(
                                type = Primitive.Type.Integer,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("petId"),
                            reference = Primitive(
                                type = Primitive.Type.Integer,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("quantity"),
                            reference = Primitive(
                                type = Primitive.Type.Integer,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("shipDate"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("status"),
                            reference = Reference.Custom(
                                value = "OrderStatus",
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("complete"),
                            reference = Primitive(
                                type = Primitive.Type.Boolean,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        )
                    )
                ),
                extends = emptyList(),
            ),
            Enum(
                comment = null,
                identifier = ClassIdentifier("OrderStatus"),
                entries = setOf("placed", "approved", "delivered"),
            ),
            Type(
                comment = null,
                identifier = ClassIdentifier("Customer"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Primitive(
                                type = Primitive.Type.Integer,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("username"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("address"),
                            reference = Reference.Custom(value = "Address", isIterable = true, isDictionary = false),
                            isNullable = true
                        )
                    )
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = ClassIdentifier("Address"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("street"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("city"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("state"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("zip"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        )
                    )
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = ClassIdentifier("Category"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Primitive(
                                type = Primitive.Type.Integer,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        )
                    )
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = ClassIdentifier("User"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Primitive(
                                type = Primitive.Type.Integer,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("username"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("firstName"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("lastName"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("email"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("password"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("phone"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("userStatus"),
                            reference = Primitive(
                                type = Primitive.Type.Integer,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        )
                    )
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = ClassIdentifier("Tag"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Primitive(
                                type = Primitive.Type.Integer,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        )
                    )
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = ClassIdentifier("Pet"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Primitive(
                                type = Primitive.Type.Integer,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = FieldIdentifier("category"),
                            reference = Reference.Custom(
                                value = "Category",
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("photoUrls"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = true,
                                isDictionary = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = FieldIdentifier("tags"),
                            reference = Reference.Custom(
                                value = "Tag",
                                isIterable = true,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("status"),
                            reference = Reference.Custom(
                                value = "PetStatus",
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        )
                    )
                ),
                extends = emptyList(),
            ),
            Enum(
                comment = null,
                identifier = ClassIdentifier("PetStatus"),
                entries = setOf("available", "pending", "sold"),
            ),
            Type(
                comment = null,
                identifier = ClassIdentifier("ApiResponse"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("code"),
                            reference = Primitive(
                                type = Primitive.Type.Integer,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("type"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = true
                        ),
                        Field(
                            identifier = FieldIdentifier("message"),
                            reference = Primitive(
                                type = Primitive.Type.String,
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

        assertEquals(expect.filterIsInstance<Type>(), ast.filterIsInstance<Type>())
        assertEquals(expect.filterIsInstance<Enum>(), ast.filterIsInstance<Enum>())

        val endpoint = ast.filterIsInstance<Endpoint>().find { it.identifier.value == "GetInventory" }

        val expectedEndpoint = Endpoint(
            comment = null,
            identifier = ClassIdentifier("GetInventory"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "store"), Endpoint.Segment.Literal(value = "inventory")),
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
                        reference = Primitive(type = Primitive.Type.Integer, isIterable = false, isDictionary = true),
                        isNullable = false
                    )
                )
            )
        )
        assertEquals(expectedEndpoint, endpoint)
    }

    @Test
    fun pizza() {
        val json = Resource("src/commonTest/resources/v3/pizza.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        val expect = listOf(
            Endpoint(
                comment = null,
                identifier = ClassIdentifier("PizzasPizzaIdIngredientsGET"),
                method = Endpoint.Method.GET,
                path = listOf(
                    Endpoint.Segment.Literal("pizzas"),
                    Endpoint.Segment.Param(
                        FieldIdentifier("pizzaId"),
                        Primitive(type = Primitive.Type.String, isIterable = false)
                    ),
                    Endpoint.Segment.Literal("ingredients"),
                ),
                queries = listOf(),
                headers = listOf(),
                cookies = listOf(),
                requests = listOf(
                    Endpoint.Request(null),
                ),
                responses = listOf(
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
                comment = null,
                identifier = ClassIdentifier("Ingredient"),
                shape = Shape(
                    listOf(
                        Field(FieldIdentifier("id"), Primitive(Primitive.Type.String, false), true),
                        Field(FieldIdentifier("name"), Primitive(Primitive.Type.String, false), true),
                        Field(FieldIdentifier("quantity"), Primitive(Primitive.Type.String, false), true),
                    )
                ),
                extends = emptyList(),
            ),
        )
        assertEquals(expect, ast)
    }

    @Test
    fun todo() {

        val json = Resource("src/commonTest/resources/v3/todo.json").readText()

        val openApi = OpenAPI.decodeFromString(json)

        val ast = openApi.parse()

        val expect = listOf(
            Endpoint(
                comment = null,
                identifier = ClassIdentifier("TodosList"),
                method = Endpoint.Method.GET,
                path = listOf(
                    Endpoint.Segment.Literal("todos")
                ),
                queries = listOf(
                    Field(FieldIdentifier("completed"), Primitive(type = Primitive.Type.Boolean, isIterable = false), true)
                ),
                headers = listOf(
                    Field(FieldIdentifier("x-user"), Primitive(type = Primitive.Type.Boolean, isIterable = false), true)
                ),
                cookies = listOf(),
                requests = listOf(
                    Endpoint.Request(null),
                ),
                responses = listOf(
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
                comment = null,
                identifier = ClassIdentifier("TodosPOST"),
                method = Endpoint.Method.POST,
                path = listOf(
                    Endpoint.Segment.Literal("todos"),
                ),
                queries = listOf(),
                headers = listOf(
                    Field(FieldIdentifier("x-user"), Primitive(type = Primitive.Type.Boolean, isIterable = false), true)
                ),
                cookies = listOf(),
                requests = listOf(
                    Endpoint.Request(
                        Endpoint.Content(
                            type = "application/json",
                            reference = Reference.Custom("Todo_input", false),
                            isNullable = true
                        )
                    ),
                    Endpoint.Request(
                        Endpoint.Content(
                            type = "application/xml",
                            reference = Reference.Custom("Todo", false),
                            isNullable = true
                        )
                    )
                ),
                responses = listOf(
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
                comment = null,
                identifier = ClassIdentifier("TodosIdGET"),
                method = Endpoint.Method.GET,
                path = listOf(
                    Endpoint.Segment.Literal("todos"),
                    Endpoint.Segment.Param(FieldIdentifier("id"), Primitive(Primitive.Type.String, false))
                ),
                queries = listOf(),
                headers = listOf(),
                cookies = listOf(),
                requests = listOf(
                    Endpoint.Request(null),
                ),
                responses = listOf(
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
                comment = null,
                identifier = ClassIdentifier("Todo_input"),
                shape = Shape(
                    listOf(
                        Field(FieldIdentifier("title"), Primitive(Primitive.Type.String, false), true),
                        Field(FieldIdentifier("completed"), Primitive(Primitive.Type.Boolean, false), true)
                    )
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = ClassIdentifier("Todo"),
                shape = Shape(
                    listOf(
                        Field(FieldIdentifier("id"), Primitive(Primitive.Type.String, false), true),
                        Field(FieldIdentifier("title"), Primitive(Primitive.Type.String, false), true),
                        Field(FieldIdentifier("completed"), Primitive(Primitive.Type.Boolean, false), true),
                        Field(FieldIdentifier("alert"), Reference.Custom("TodoAlert", false), true),
                    )
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = ClassIdentifier("TodoAlert"),
                shape = Shape(
                    listOf(
                        Field(FieldIdentifier("code"), Primitive(Primitive.Type.String, false), true),
                        Field(FieldIdentifier("message"), Reference.Custom("TodoAlertMessage", false), true),
                    )
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = ClassIdentifier("TodoAlertMessage"),
                shape = Shape(
                    listOf(
                        Field(FieldIdentifier("key"), Primitive(Primitive.Type.String, false), true),
                        Field(FieldIdentifier("value"), Primitive(Primitive.Type.String, false), true),
                    )
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = ClassIdentifier("TodosnestedArray"),
                shape = Shape(
                    listOf(
                        Field(FieldIdentifier("id"), Primitive(Primitive.Type.String, false), true),
                        Field(FieldIdentifier("title"), Primitive(Primitive.Type.String, false), true),
                        Field(FieldIdentifier("nested"), Primitive(Primitive.Type.Boolean, false), true),
                    )
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = ClassIdentifier("Error"),
                shape = Shape(
                    listOf(
                        Field(FieldIdentifier("code"), Primitive(Primitive.Type.String, false), true),
                        Field(FieldIdentifier("message"), Primitive(Primitive.Type.String, false), true),
                    )
                ),
                extends = emptyList(),
            ),
        )

        assertEquals(expect, ast)
    }

    @Test
    fun objectInRequest() {
        val json = Resource("src/commonTest/resources/v3/object-in-request.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Expected.objectInRequest, ast)

        println(ast)
    }

    @Test
    fun objectInResponse() {
        val json = Resource("src/commonTest/resources/v3/object-in-response.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Expected.objectInResponse, ast)

        println(ast)
    }

    @Test
    fun additionalProperties() {
        val json = Resource("src/commonTest/resources/v3/additionalproperties.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Expected.additionalProperties, ast)

        println(ast)
    }

    @Test
    fun array() {
        val json = Resource("src/commonTest/resources/v3/array.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Expected.array, ast)

        println(ast)
    }

    @Test
    fun allOf() {
        val json = Resource("src/commonTest/resources/v3/allof.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Expected.allOf, ast)
    }

    @Test
    fun oneOf() {
        val json = Resource("src/commonTest/resources/v3/oneof.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        Expected.oneOf.zip(ast).forEach { (expected, actual) ->
            println(expected.identifier)
            assertEquals(expected, actual)
        }

    }

    @Test
    fun enum() {
        val json = Resource("src/commonTest/resources/v3/enum.json").readText()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Expected.enum, ast)
    }
}
