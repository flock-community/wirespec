package community.flock.wirespec.openapi.v3

import community.flock.kotlinx.openapi.bindings.v3.OpenAPI
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Identifier
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.openapi.IO
import kotlin.test.Test
import kotlin.test.assertEquals


class OpenApiParserTest {

    @Test
    fun pizza() {
        val json = IO.readOpenApi("v3/pizza.json")

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
                        "200",
                        Endpoint.Content("application/json", Reference.Custom("Ingredient", true), false)
                    ),
                    Endpoint.Response(
                        "404",
                        null
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

        val json = IO.readOpenApi("v3/todo.json")

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
                    Field(Identifier("completed"), Primitive(type = Primitive.Type.Boolean, isIterable = false), false)
                ),
                listOf(
                    Field(Identifier("x-user"), Primitive(type = Primitive.Type.Boolean, isIterable = false), false)
                ),
                listOf(),
                listOf(
                    Endpoint.Request(null),
                ),
                listOf(
                    Endpoint.Response(
                        "200",
                        Endpoint.Content("application/json", Reference.Custom("Todo", true), false)
                    ),
                    Endpoint.Response(
                        "500",
                        Endpoint.Content("application/json", Reference.Custom("Error", false), false)
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
                    Field(Identifier("x-user"), Primitive(type = Primitive.Type.Boolean, isIterable = false), false)
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
                    Endpoint.Response("201", null),
                    Endpoint.Response(
                        "500", Endpoint.Content(
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
                        content = Endpoint.Content(
                            type = "application/json",
                            reference = Reference.Custom("Todo", false),
                            isNullable = true
                        )
                    ),
                    Endpoint.Response(
                        status = "500",
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
                        Field(Identifier(value = "title"), Primitive(Primitive.Type.String, false), true),
                        Field(Identifier(value = "completed"), Primitive(Primitive.Type.Boolean, false), true)
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

        val emitter = KotlinEmitter()
        val output = emitter.emit(ast)

        println("-------------")
        println(output.joinToString("\n") { it.second })
        println("-------------")

        assertEquals(expect, ast)
    }
}
