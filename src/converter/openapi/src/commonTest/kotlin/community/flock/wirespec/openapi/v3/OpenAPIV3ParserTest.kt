package community.flock.wirespec.openapi.v3

import arrow.core.nonEmptyListOf
import community.flock.kotlinx.openapi.bindings.OpenAPIV3
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.ast.Annotation
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Reference.Custom
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Type.Shape
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.openapi.common.Ast
import community.flock.wirespec.openapi.common.toDescriptionAnnotationList
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser.parse
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test

class OpenAPIV3ParserTest {

    @Test
    fun petstore() {
        val path = Path("src/commonTest/resources/v3/petstore.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        val expect = listOf(
            Enum(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("FindPetsByStatusParameterStatus"),
                entries = setOf("available", "pending", "sold"),
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
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64, null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("petId"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64, null),
                                isNullable = true,
                            ),
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
                            reference = Custom(
                                value = "OrderStatus",
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("complete"),
                            annotations = emptyList(),
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
                annotations = emptyList(),
                identifier = DefinitionIdentifier("OrderStatus"),
                entries = setOf("placed", "approved", "delivered"),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Customer"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64, null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("username"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("address"),
                            annotations = emptyList(),
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
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Address"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("street"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("city"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("state"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("zip"),
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
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Category"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64, null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
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
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("User"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64, null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("username"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("firstName"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("lastName"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("email"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("password"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("phone"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("userStatus"),
                            annotations = "User Status".toDescriptionAnnotationList(),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P32, null),
                                isNullable = true,
                            ),
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
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64, null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
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
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Pet"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64, null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("category"),
                            annotations = emptyList(),
                            reference = Custom(
                                value = "Category",
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("photoUrls"),
                            annotations = emptyList(),
                            reference = Reference.Iterable(
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
                            annotations = emptyList(),
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
                annotations = emptyList(),
                comment = null,
                identifier = DefinitionIdentifier("PetStatus"),
                entries = setOf("available", "pending", "sold"),
            ),
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
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("message"),
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

        ast.filterIsInstance<Type>() shouldBe expect.filterIsInstance<Type>()
        ast.filterIsInstance<Enum>() shouldBe expect.filterIsInstance<Enum>()

        val endpoint = ast.filterIsInstance<Endpoint>().find { it.identifier.value == "GetInventory" }

        val expectedEndpoint = Endpoint(
            comment = null,
            annotations = "Returns a map of status codes to quantities".toDescriptionAnnotationList(),
            identifier = DefinitionIdentifier("GetInventory"),
            method = Endpoint.Method.GET,
            path = listOf(Endpoint.Segment.Literal(value = "store"), Endpoint.Segment.Literal(value = "inventory")),
            queries = emptyList(),
            headers = emptyList(),
            requests = listOf(Endpoint.Request(content = null)),
            responses = listOf(
                Endpoint.Response(
                    annotations = "successful operation".toDescriptionAnnotationList(),
                    status = "200",
                    headers = emptyList(),
                    content = Endpoint.Content(
                        type = "application/json",
                        reference = Reference.Dict(
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P32, null),
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
        )
        endpoint shouldBe expectedEndpoint
    }

    @Test
    fun pizza() {
        val path = Path("src/commonTest/resources/v3/pizza.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        val expect = listOf(
            Endpoint(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("PizzasPizzaIdIngredientsGET"),
                method = Endpoint.Method.GET,
                path = listOf(
                    Endpoint.Segment.Literal("pizzas"),
                    Endpoint.Segment.Param(
                        FieldIdentifier("pizzaId"),
                        Primitive(type = Primitive.Type.String(null), isNullable = false),
                    ),
                    Endpoint.Segment.Literal("ingredients"),
                ),
                queries = listOf(),
                headers = listOf(),
                requests = listOf(
                    Endpoint.Request(null),
                ),
                responses = listOf(
                    Endpoint.Response(
                        annotations = "Ok".toDescriptionAnnotationList(),
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
                        annotations = "NotFound".toDescriptionAnnotationList(),
                        status = "404",
                        headers = emptyList(),
                        content = null,
                    ),
                ),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Ingredient"),
                shape = Shape(
                    listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), true),
                        ),
                        Field(
                            identifier = FieldIdentifier("name"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), true),
                        ),
                        Field(
                            identifier = FieldIdentifier("quantity"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
        )
        ast shouldBe expect
    }

    @Test
    fun todo() {
        val path = Path("src/commonTest/resources/v3/todo.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)

        val ast = openApi.parse().shouldNotBeNull()

        val expect = listOf(
            Endpoint(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("TodosList"),
                method = Endpoint.Method.GET,
                path = listOf(
                    Endpoint.Segment.Literal("todos"),
                ),
                queries = listOf(
                    Field(
                        identifier = FieldIdentifier("completed"),
                        annotations = emptyList(),
                        reference = Primitive(type = Primitive.Type.Boolean, isNullable = true),
                    ),
                ),
                headers = listOf(
                    Field(
                        identifier = FieldIdentifier("x-user"),
                        annotations = emptyList(),
                        reference = Primitive(type = Primitive.Type.Boolean, isNullable = true),
                    ),
                ),
                requests = listOf(
                    Endpoint.Request(null),
                ),
                responses = listOf(
                    Endpoint.Response(
                        annotations = "OK".toDescriptionAnnotationList(),
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
                        annotations = "Error".toDescriptionAnnotationList(),
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
                annotations = emptyList(),
                identifier = DefinitionIdentifier("TodosPOST"),
                method = Endpoint.Method.POST,
                path = listOf(
                    Endpoint.Segment.Literal("todos"),
                ),
                queries = listOf(),
                headers = listOf(
                    Field(
                        identifier = FieldIdentifier("x-user"),
                        annotations = emptyList(),
                        reference = Primitive(type = Primitive.Type.Boolean, isNullable = true),
                    ),
                ),
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
                        annotations = "OkNotContent".toDescriptionAnnotationList(),
                        status = "201",
                        headers = emptyList(),
                        content = null,
                    ),
                    Endpoint.Response(
                        annotations = "Error".toDescriptionAnnotationList(),
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
                annotations = emptyList(),
                identifier = DefinitionIdentifier("TodosIdGET"),
                method = Endpoint.Method.GET,
                path = listOf(
                    Endpoint.Segment.Literal("todos"),
                    Endpoint.Segment.Param(
                        identifier = FieldIdentifier("id"),
                        reference = Primitive(Primitive.Type.String(null), false),
                    ),
                ),
                queries = listOf(),
                headers = listOf(),
                requests = listOf(
                    Endpoint.Request(null),
                ),
                responses = listOf(
                    Endpoint.Response(
                        annotations = "OK".toDescriptionAnnotationList(),
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
                        annotations = "Error".toDescriptionAnnotationList(),
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
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Todo_input"),
                shape = Shape(
                    listOf(
                        Field(
                            identifier = FieldIdentifier("title"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), true),
                        ),
                        Field(
                            identifier = FieldIdentifier("completed"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.Boolean, true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Todo"),
                shape = Shape(
                    listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), true),
                        ),
                        Field(
                            identifier = FieldIdentifier("title"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), true),
                        ),
                        Field(
                            identifier = FieldIdentifier("completed"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.Boolean, true),
                        ),
                        Field(
                            identifier = FieldIdentifier("alert"),
                            annotations = emptyList(),
                            reference = Custom("TodoAlert", true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("TodoAlert"),
                shape = Shape(
                    listOf(
                        Field(
                            identifier = FieldIdentifier("code"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), true),
                        ),
                        Field(
                            identifier = FieldIdentifier("message"),
                            annotations = emptyList(),
                            reference = Custom("TodoAlertMessage", true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("TodoAlertMessage"),
                shape = Shape(
                    listOf(
                        Field(
                            identifier = FieldIdentifier("key"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), true),
                        ),
                        Field(
                            identifier = FieldIdentifier("value"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("TodosnestedArray"),
                shape = Shape(
                    listOf(
                        Field(
                            identifier =
                            FieldIdentifier("id"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), true),
                        ),
                        Field(
                            identifier = FieldIdentifier("title"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), true),
                        ),
                        Field(
                            identifier = FieldIdentifier("nested"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.Boolean, true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Error"),
                shape = Shape(
                    listOf(
                        Field(
                            identifier = FieldIdentifier("code"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), isNullable = true),
                        ),
                        Field(
                            identifier = FieldIdentifier("message"),
                            annotations = emptyList(),
                            reference = Primitive(Primitive.Type.String(null), true),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
        )

        ast shouldBe expect
    }

    @Test
    fun objectInRequest() {
        val path = Path("src/commonTest/resources/v3/object-in-request.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        ast shouldBe Ast.objectInRequest
    }

    @Test
    fun objectInResponse() {
        val path = Path("src/commonTest/resources/v3/object-in-response.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        ast shouldBe Ast.objectInResponse
    }

    @Test
    fun additionalProperties() {
        val path = Path("src/commonTest/resources/v3/additionalproperties.json")
        val json = SystemFileSystem.source(path).buffered().readString()
        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        ast shouldBe Ast.additionalProperties
    }

    @Test
    fun array() {
        val path = Path("src/commonTest/resources/v3/array.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        ast shouldBe Ast.array
    }

    @Test
    fun allOf() {
        val path = Path("src/commonTest/resources/v3/allof.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        ast shouldBe Ast.allOf
    }

    @Test
    fun oneOf() {
        val path = Path("src/commonTest/resources/v3/oneof.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        Ast.oneOf.zip(ast).forEach { (expected, actual) ->
            println(expected.identifier)
            actual shouldBe expected
        }
    }

    @Test
    fun enum() {
        val path = Path("src/commonTest/resources/v3/enum.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        ast shouldBe Ast.enum
    }

    @Test
    fun responseref() {
        val path = Path("src/commonTest/resources/v3/responseref.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        val expected = listOf(
            Endpoint(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name = "ResponserefGET"),
                method = Endpoint.Method.GET,
                path = listOf(Endpoint.Segment.Literal(value = "responseref")),
                queries = emptyList(),
                headers = emptyList(),
                requests = listOf(
                    Endpoint.Request(content = null),
                ),
                responses = listOf(
                    Endpoint.Response(
                        annotations = "An Address".toDescriptionAnnotationList(),
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
                        annotations = "An Address".toDescriptionAnnotationList(),
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
                annotations = emptyList(),
                identifier = DefinitionIdentifier(
                    name = "ResponserefGET202ResponseBody",
                ),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier(name = "me"),
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
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name = "Address"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier(name = "streetName"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "houseNumber"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(
                                    constraint = Primitive.Type.Constraint.RegExp("^[\\d-]+\$"),
                                ),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(
                                name = "houseNumberExtension",
                            ),
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
    fun queryref() {
        val path = Path("src/commonTest/resources/v3/queryref.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        val expected = listOf(
            Endpoint(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name = "ResponserefGET"),
                method = Endpoint.Method.GET,
                path = listOf(Endpoint.Segment.Literal(value = "responseref")),
                queries = listOf(
                    Field(
                        identifier = FieldIdentifier(name = "embed"),
                        annotations = emptyList(),
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
                        annotations = emptyList(),
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
                requests = listOf(Endpoint.Request(content = null)),
                responses = listOf(
                    Endpoint.Response(
                        annotations = "Success".toDescriptionAnnotationList(),
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
                annotations = emptyList(),
                identifier = DefinitionIdentifier(
                    name = "ResponserefGETParameterEmbedArray",
                ),
                entries = setOf("links"),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name = "ResponserefGET201ResponseBody"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier(name = "test"),
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
            Enum(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name = "EmbedParamsArray"),
                entries = setOf("links"),
            ),
        )
        ast shouldBe expected
    }

    @Test
    fun refarray() {
        val path = Path("src/commonTest/resources/v3/refarray.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        val expected = listOf(
            Endpoint(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name = "RefarrayGET"),
                method = Endpoint.Method.GET,
                path = listOf(Endpoint.Segment.Literal(value = "refarray")),
                queries = emptyList(),
                headers = emptyList(),
                requests = listOf(Endpoint.Request(content = null)),
                responses = listOf(
                    Endpoint.Response(
                        annotations = "Proposals".toDescriptionAnnotationList(),
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
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name = "RefarrayGET200ResponseBody"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier(name = "proposals"),
                            annotations = emptyList(),
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
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.Integer(Primitive.Type.Precision.P64, null),
                                isNullable = false,
                            ),
                        ),
                    ),
                ),
                extends = emptyList(),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name = "Proposal"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier(name = "id"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(
                                    constraint = Primitive.Type.Constraint.RegExp("^\\d+\$"),
                                ),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "status"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "author"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "reviewer"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "updatedAt"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(
                                    constraint = null,
                                ),
                                isNullable = false,
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
    fun refprimary() {
        val path = Path("src/commonTest/resources/v3/refprimary.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        val expected = listOf(
            Endpoint(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name = "RefprimaryGET"),
                method = Endpoint.Method.GET,
                path = listOf(Endpoint.Segment.Literal(value = "refprimary")),
                queries = emptyList(),
                headers = emptyList(),
                requests = listOf(Endpoint.Request(content = null)),
                responses = listOf(
                    Endpoint.Response(
                        annotations = "Proposals".toDescriptionAnnotationList(),
                        status = "200",
                        headers = emptyList(),
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Primitive(
                                type = Primitive.Type.String(
                                    constraint = Primitive.Type.Constraint.RegExp("^\\d+\$"),
                                ),
                                isNullable = false,
                            ),
                        ),
                    ),
                ),
            ),
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name = "Address"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier(name = "entityId"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(
                                    constraint = Primitive.Type.Constraint.RegExp("^\\d+\$"),
                                ),
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
    fun deeparraysimpl() {
        val path = Path("src/commonTest/resources/v3/deeparraysimple.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        val expected = nonEmptyListOf(
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name = "User"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier(name = "email"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(
                                    constraint = null,
                                ),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "name"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = true,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier(name = "permissions"),
                            annotations = emptyList(),
                            reference = Reference.Iterable(
                                reference = Primitive(
                                    type = Primitive.Type.String(null),
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
        ast shouldBe expected
    }

    @Test
    fun componentsResponses() {
        val path = Path("src/commonTest/resources/v3/components-responses.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
        val ast = openApi.parse().shouldNotBeNull()

        val expected = nonEmptyListOf(
            Endpoint(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name = "OneofGET"),
                method = Endpoint.Method.GET,
                path = listOf(
                    Endpoint.Segment.Literal(value = "oneof"),
                ),
                queries = emptyList(),
                headers = emptyList(),
                requests = listOf(
                    Endpoint.Request(content = null),
                ),
                responses = listOf(
                    Endpoint.Response(
                        annotations = "Created contact".toDescriptionAnnotationList(),
                        status = "200",
                        headers = emptyList(),
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Custom(value = "OneofGET200ResponseBody", isNullable = false),
                        ),
                    ),
                ),
            ),
            Union(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier(name = "OneofGET200ResponseBody"),
                entries = setOf(
                    Custom(value = "Foo", isNullable = false),
                    Custom(value = "Bar", isNullable = false),
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
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("Bar"),
                shape = Shape(
                    value = listOf(
                        Field(
                            identifier = FieldIdentifier("b"),
                            annotations = emptyList(),
                            reference = Primitive(
                                type = Primitive.Type.String(null),
                                isNullable = false,
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
    fun emptyResponses() {
        val path = Path("src/commonTest/resources/v3/empty-response.json")
        val json = SystemFileSystem.source(path).buffered().readString()

        val openApi = OpenAPIV3.decodeFromString(json)
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
                        annotations = "Ok".toDescriptionAnnotationList(),
                        status = "200",
                        headers = emptyList(),
                        content = null,
                    ),
                ),
            ),
        )
        ast shouldBe expected
    }

    @Test
    fun testDescriptionParsing() {
        val json =
            """
            |{
            |    "openapi": "3.0.0",
            |    "info": {
            |        "title": "Test API",
            |        "version": "1.0.0"
            |    },
            |    "components": {
            |        "schemas": {
            |            "Todo": {
            |                "description": "Todo object",
            |                "type": "object",
            |                "properties": {
            |                    "id": {
            |                        "type": "string",
            |                        "description": "id field"
            |                    }
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

        val ast = OpenAPIV3Parser.parse(ModuleContent(FileUri("test.json"), json), false)
        val definitions = ast.modules.head.statements
        println(definitions.map { it.identifier.value })

        val todo = definitions.find { (it as? Type)?.identifier?.value == "Todo" }.shouldBeInstanceOf<Type>()
        todo.annotations shouldContain Annotation(
            "Description",
            listOf(Annotation.Parameter("default", Annotation.Value.Single("Todo object"))),
        )

        val idField = todo.shape.value.find { it.identifier.value == "id" }!!
        idField.annotations shouldContain Annotation(
            "Description",
            listOf(Annotation.Parameter("default", Annotation.Value.Single("id field"))),
        )

        val endpoint =
            definitions.find { (it as? Endpoint)?.identifier?.value == "TodosGET" }.shouldBeInstanceOf<Endpoint>()
        endpoint.annotations shouldContain Annotation(
            "Description",
            listOf(Annotation.Parameter("default", Annotation.Value.Single("Get all todos"))),
        )
    }

    @Test
    fun nameCollision() {
        val json =
            """
            |{
            |    "openapi": "3.0.0",
            |    "info": {
            |        "title": "Test API",
            |        "version": "1.0.0"
            |    },
            |    "components": {
            |        "schemas": {
            |            "Pet": {
            |                "type": "object",
            |                "properties": {
            |                    "id": {
            |                        "type": "string"
            |                    }
            |                }
            |            }
            |        }
            |    },
            |    "paths": {
            |        "/pets": {
            |            "get": {
            |                "operationId": "Pet",
            |                "responses": {
            |                    "200": {
            |                        "description": "Ok"
            |                    }
            |                }
            |            }
            |        }
            |    }
            |}
            """.trimMargin()

        val ast = OpenAPIV3Parser.parse(ModuleContent(FileUri("test.json"), json), false)
        val definitions = ast.modules.head.statements

        val type = definitions.find { (it as? Type)?.identifier?.value == "Pet" }.shouldBeInstanceOf<Type>()
        type.identifier.value shouldBe "Pet"

        val endpoint = definitions.find { it is Endpoint }.shouldBeInstanceOf<Endpoint>()
        endpoint.identifier.value shouldBe "PetEndpoint"
    }
}
