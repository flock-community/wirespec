package community.flock.wirespec.openapi.v3

import community.flock.kotlinx.openapi.bindings.v3.OpenAPI
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Reference.Custom
import community.flock.wirespec.compiler.core.parse.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.openapi.common.Ast
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser.parse
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenAPIV3ParserTest {

    @Test
    fun petstore() {
        val path = Path("src/commonTest/resources/v3/petstore.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        val expect = listOf(
            Enum(
                comment = null,
                identifier = DefinitionIdentifier("FindPetsByStatusParameterStatus"),
                entries = setOf("available", "pending", "sold"),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("Order"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("petId"),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64),
                                isNullable = true,
                            ),
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
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("status"),
                            reference = Custom(
                                value = "OrderStatus",
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("complete"),
                            reference = Primitive(
                                type = Primitive.Type.Boolean,
                                isNullable = true,
                            ),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Enum(
                comment = null,
                identifier = DefinitionIdentifier("OrderStatus"),
                entries = setOf("placed", "approved", "delivered"),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("Customer"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("username"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("address"),
                            reference = Reference.Iterable(
                                reference = Custom(
                                    value = "Address",
                                    isNullable = false,
                                ),
                                isNullable = true,
                            ),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("Address"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("street"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("city"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("state"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("zip"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
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
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
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
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("username"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("firstName"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("lastName"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("email"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("password"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("phone"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
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
            Type(
                comment = null,
                identifier = DefinitionIdentifier("Tag"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
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
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("category"),
                            reference = Custom(
                                value = "Category",
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("photoUrls"),
                            reference = Reference.Iterable(
                                reference = Primitive(
                                    type = Primitive.Type.String,
                                    isNullable = false,
                                ),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("tags"),
                            reference = Reference.Iterable(
                                reference = Custom(
                                    value = "Tag",
                                    isNullable = false,
                                ),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("status"),
                            reference = Custom(
                                value = "PetStatus",
                                isNullable = true,
                            ),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Enum(
                comment = null,
                identifier = DefinitionIdentifier("PetStatus"),
                entries = setOf("available", "pending", "sold"),
            ),
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
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("message"),
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

        assertEquals(expect.filterIsInstance<Type>(), ast.filterIsInstance<Type>())
        assertEquals(expect.filterIsInstance<Enum>(), ast.filterIsInstance<Enum>())

        val endpoint = ast.filterIsInstance<Endpoint>().find { it.identifier.value == "GetInventory" }

        val expectedEndpoint = Endpoint(
            comment = null,
            identifier = DefinitionIdentifier("GetInventory"),
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
                        reference = Reference.Dict(
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P32),
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
        )
        assertEquals(expectedEndpoint, endpoint)
    }

    @Test
    fun pizza() {
        val path = Path("src/commonTest/resources/v3/pizza.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        val expect = listOf(
            Endpoint(
                comment = null,
                identifier = DefinitionIdentifier("PizzasPizzaIdIngredientsGET"),
                method = Endpoint.Method.GET,
                path = listOf(
                    Endpoint.Segment.Literal("pizzas"),
                    Endpoint.Segment.Param(
                        FieldIdentifier("pizzaId"),
                        Primitive(type = Primitive.Type.String, isNullable = false),
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
                        content = Endpoint.Content(
                            "application/json",
                            Reference.Iterable(
                                reference = Custom(
                                    value = "Ingredient",
                                    isNullable = false,
                                ),
                                isNullable = false,
                            ),
                        ),
                    ),
                    Endpoint.Response(
                        status = "404",
                        headers = emptyList(),
                        content = null,
                    ),
                ),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("Ingredient"),
                shape = Shape(
                    listOf(
                        Field(FieldIdentifier("id"), Primitive(Primitive.Type.String, true)),
                        Field(FieldIdentifier("name"), Primitive(Primitive.Type.String, true)),
                        Field(FieldIdentifier("quantity"), Primitive(Primitive.Type.String, true)),
                    ),
                ),
                extends = emptyList(),
            ),
        )
        assertEquals(expect, ast)
    }

    @Test
    fun todo() {
        val path = Path("src/commonTest/resources/v3/todo.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)

        val ast = openApi.parse()

        val expect = listOf(
            Endpoint(
                comment = null,
                identifier = DefinitionIdentifier("TodosList"),
                method = Endpoint.Method.GET,
                path = listOf(
                    Endpoint.Segment.Literal("todos"),
                ),
                queries = listOf(
                    Field(
                        FieldIdentifier("completed"),
                        Primitive(type = Primitive.Type.Boolean, isNullable = true),
                    ),
                ),
                headers = listOf(
                    Field(
                        FieldIdentifier("x-user"),
                        Primitive(type = Primitive.Type.Boolean, isNullable = true),
                    ),
                ),
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
                            reference = Reference.Iterable(
                                reference = Custom(
                                    value = "Todo",
                                    isNullable = false,
                                ),

                                isNullable = false,
                            ),
                        ),
                    ),
                    Endpoint.Response(
                        status = "500",
                        headers = emptyList(),
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Custom(
                                value = "Error",
                                isNullable = false,
                            ),
                        ),
                    ),
                ),
            ),
            Endpoint(
                comment = null,
                identifier = DefinitionIdentifier("TodosPOST"),
                method = Endpoint.Method.POST,
                path = listOf(
                    Endpoint.Segment.Literal("todos"),
                ),
                queries = listOf(),
                headers = listOf(
                    Field(
                        FieldIdentifier("x-user"),
                        Primitive(type = Primitive.Type.Boolean, isNullable = true),
                    ),
                ),
                cookies = listOf(),
                requests = listOf(
                    Endpoint.Request(
                        Endpoint.Content(
                            type = "application/json",
                            reference = Custom(
                                value = "Todo_input",
                                isNullable = false,
                            ),
                        ),
                    ),
                    Endpoint.Request(
                        Endpoint.Content(
                            type = "application/xml",
                            reference = Custom(
                                value = "Todo",
                                isNullable = false,
                            ),
                        ),
                    ),
                ),
                responses = listOf(
                    Endpoint.Response(
                        status = "201",
                        headers = emptyList(),
                        content = null,
                    ),
                    Endpoint.Response(
                        status = "500",
                        headers = emptyList(),
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Custom(
                                value = "Error",
                                isNullable = false,
                            ),
                        ),
                    ),
                ),
            ),
            Endpoint(
                comment = null,
                identifier = DefinitionIdentifier("TodosIdGET"),
                method = Endpoint.Method.GET,
                path = listOf(
                    Endpoint.Segment.Literal("todos"),
                    Endpoint.Segment.Param(FieldIdentifier("id"), Primitive(Primitive.Type.String, false)),
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
                            reference = Custom(
                                value = "Todo",
                                isNullable = true,
                            ),
                        ),
                    ),
                    Endpoint.Response(
                        status = "500",
                        headers = emptyList(),
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Custom(
                                value = "Error",
                                isNullable = false,
                            ),
                        ),
                    ),
                ),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("Todo_input"),
                shape = Shape(
                    listOf(
                        Field(FieldIdentifier("title"), Primitive(Primitive.Type.String, true)),
                        Field(FieldIdentifier("completed"), Primitive(Primitive.Type.Boolean, true)),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("Todo"),
                shape = Shape(
                    listOf(
                        Field(FieldIdentifier("id"), Primitive(Primitive.Type.String, true)),
                        Field(FieldIdentifier("title"), Primitive(Primitive.Type.String, true)),
                        Field(FieldIdentifier("completed"), Primitive(Primitive.Type.Boolean, true)),
                        Field(FieldIdentifier("alert"), Custom("TodoAlert", true)),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("TodoAlert"),
                shape = Shape(
                    listOf(
                        Field(FieldIdentifier("code"), Primitive(Primitive.Type.String, true)),
                        Field(FieldIdentifier("message"), Custom("TodoAlertMessage", true)),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("TodoAlertMessage"),
                shape = Shape(
                    listOf(
                        Field(FieldIdentifier("key"), Primitive(Primitive.Type.String, true)),
                        Field(FieldIdentifier("value"), Primitive(Primitive.Type.String, true)),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("TodosnestedArray"),
                shape = Shape(
                    listOf(
                        Field(FieldIdentifier("id"), Primitive(Primitive.Type.String, true)),
                        Field(FieldIdentifier("title"), Primitive(Primitive.Type.String, true)),
                        Field(FieldIdentifier("nested"), Primitive(Primitive.Type.Boolean, true)),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier("Error"),
                shape = Shape(
                    listOf(
                        Field(FieldIdentifier("code"), Primitive(Primitive.Type.String, true)),
                        Field(FieldIdentifier("message"), Primitive(Primitive.Type.String, true)),
                    ),
                ),
                extends = emptyList(),
            ),
        )

        assertEquals(expect, ast)
    }

    @Test
    fun objectInRequest() {
        val path = Path("src/commonTest/resources/v3/object-in-request.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Ast.objectInRequest, ast)

        println(ast)
    }

    @Test
    fun objectInResponse() {
        val path = Path("src/commonTest/resources/v3/object-in-response.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Ast.objectInResponse, ast)

        println(ast)
    }

    @Test
    fun additionalProperties() {
        val path = Path("src/commonTest/resources/v3/additionalproperties.json")
        val json = SystemFileSystem.source(path).buffered().readString()
        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Ast.additionalProperties, ast)

        println(ast)
    }

    @Test
    fun array() {
        val path = Path("src/commonTest/resources/v3/array.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Ast.array, ast)

        println(ast)
    }

    @Test
    fun allOf() {
        val path = Path("src/commonTest/resources/v3/allof.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Ast.allOf, ast)
    }

    @Test
    fun oneOf() {
        val path = Path("src/commonTest/resources/v3/oneof.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        Ast.oneOf.zip(ast).forEach { (expected, actual) ->
            println(expected.identifier)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun enum() {
        val path = Path("src/commonTest/resources/v3/enum.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        assertEquals(Ast.enum, ast)
    }

    @Test
    fun responseref() {
        val path = Path("src/commonTest/resources/v3/responseref.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        val expected = listOf(
            Endpoint(
                comment = null,
                identifier = DefinitionIdentifier(name = "ResponserefGET"),
                method = Endpoint.Method.GET,
                path = listOf(Endpoint.Segment.Literal(value = "responseref")),
                queries = emptyList(),
                headers = emptyList(),
                cookies = emptyList(),
                requests = listOf(
                    Endpoint.Request(content = null),
                ),
                responses = listOf(
                    Endpoint.Response(
                        status = "201",
                        headers = emptyList(),
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Custom(
                                value = "Address",
                                isNullable = false,
                            ),
                        ),
                    ),
                    Endpoint.Response(
                        status = "202",
                        headers = emptyList(),
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Custom(
                                value = "ResponserefGET202ResponseBody",
                                isNullable = false,
                            ),
                        ),
                    ),
                ),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier(
                    name = "ResponserefGET202ResponseBody",
                ),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier(name = "me"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier(name = "Address"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier(name = "streetName"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "houseNumber"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(
                                name = "houseNumberExtension",
                            ),
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
    fun queryref() {
        val path = Path("src/commonTest/resources/v3/queryref.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        val expected = listOf(
            Endpoint(
                comment = null,
                identifier = DefinitionIdentifier(name = "ResponserefGET"),
                method = Endpoint.Method.GET,
                path = listOf(Endpoint.Segment.Literal(value = "responseref")),
                queries = listOf(
                    Field(
                        identifier = FieldIdentifier(name = "embed"),
                        reference = Reference.Iterable(
                            reference = Custom(
                                value = "ResponserefGETParameterEmbedArray",
                                isNullable = false,
                            ),
                            isNullable = true,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier(name = "embedRef"),
                        reference = Reference.Iterable(
                            reference = Custom(
                                value = "EmbedParamsArray",
                                isNullable = false,
                            ),
                            isNullable = true,
                        ),
                    ),
                ),
                headers = emptyList(),
                cookies = emptyList(),
                requests = listOf(Endpoint.Request(content = null)),
                responses = listOf(
                    Endpoint.Response(
                        status = "201",
                        headers = emptyList(),
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Custom(
                                value = "ResponserefGET201ResponseBody",
                                isNullable = false,
                            ),
                        ),
                    ),
                ),
            ),
            Enum(
                comment = null,
                identifier = DefinitionIdentifier(
                    name = "ResponserefGETParameterEmbedArray",
                ),
                entries = setOf("links"),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier(name = "ResponserefGET201ResponseBody"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier(name = "test"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Enum(
                comment = null,
                identifier = DefinitionIdentifier(name = "EmbedParamsArray"),
                entries = setOf("links"),
            ),
        )
        assertEquals(expected, ast)
    }

    @Test
    fun refarray() {
        val path = Path("src/commonTest/resources/v3/refarray.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        val expected = listOf(
            Endpoint(
                comment = null,
                identifier = DefinitionIdentifier(name = "RefarrayGET"),
                method = Endpoint.Method.GET,
                path = listOf(Endpoint.Segment.Literal(value = "refarray")),
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
                            reference = Custom(
                                value = "RefarrayGET200ResponseBody",
                                isNullable = false,
                            ),
                        ),
                    ),
                ),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier(name = "RefarrayGET200ResponseBody"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier(name = "proposals"),
                            reference = Reference.Iterable(
                                reference = Custom(
                                    value = "Proposal",
                                    isNullable = false,
                                ),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "count"),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64),
                                isNullable = false,
                            ),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier(name = "Proposal"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier(name = "id"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "status"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "author"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "reviewer"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "updatedAt"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = false,
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
    fun refprimary() {
        val path = Path("src/commonTest/resources/v3/refprimary.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        val expected = listOf(
            Endpoint(
                comment = null,
                identifier = DefinitionIdentifier(name = "RefprimaryGET"),
                method = Endpoint.Method.GET,
                path = listOf(Endpoint.Segment.Literal(value = "refprimary")),
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
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = false,
                            ),
                        ),
                    ),
                ),
            ),
            Type(
                comment = null,
                identifier = DefinitionIdentifier(name = "Address"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier(name = "entityId"),
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
    fun deeparraysimpl() {
        val path = Path("src/commonTest/resources/v3/deeparraysimple.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPI.decodeFromString(json)
        val ast = openApi.parse()

        val expected: AST = listOf(
            Type(
                comment = null,
                identifier = DefinitionIdentifier(name = "User"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier(name = "email"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "name"),
                            reference = Primitive(
                                type = Primitive.Type.String,
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "permissions"),
                            reference = Reference.Iterable(
                                reference = Primitive(
                                    type = Primitive.Type.String,
                                    isNullable = false,
                                ),
                                isNullable = false,
                            ),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
        )
        assertEquals(expected, ast)
    }
}
