package community.flock.wirespec.openapi

import community.flock.kotlinx.openapi.bindings.OpenAPI
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Identifier
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import kotlin.test.Test
import kotlin.test.assertEquals


expect object IO {
    fun readOpenApi(file: String): String
}

class OpenApiParserTest {

    @Test
    fun start() {

        val json = IO.readOpenApi("todo.json")

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
                    Endpoint.Request(content = null)
                ),
                listOf(
                    Endpoint.Response("200", Endpoint.Content("application/json", Reference.Custom("Todo", true))),
                    Endpoint.Response("500", Endpoint.Content("application/json", Reference.Custom("Error", false)))
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
                            reference = Reference.Custom("Todo_input", false)
                        )
                    ),
                    Endpoint.Request(
                        Endpoint.Content(
                            type = "application/xml",
                        reference = Reference.Custom("Todo", false)
                    )
                    )
                ),
                listOf(
                    Endpoint.Response("201", null),
                    Endpoint.Response("500", Endpoint.Content("application/json", Reference.Custom("Error", false)))
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
                    Endpoint.Request(null)
                ),
                listOf(
                    Endpoint.Response("200", Endpoint.Content("application/json", Reference.Custom("Todo", false))),
                    Endpoint.Response("500", Endpoint.Content("application/json", Reference.Custom("Error", false)))
                )
            ),
            Type(
                "Todo_input",
                Shape(
                    listOf(
                        Field(Identifier(value="title"), Primitive(Primitive.Type.String, false), false),
                        Field(Identifier(value="completed"), Primitive(Primitive.Type.Boolean, false), false)
                    )
                )
            ),
            Type(
                "Todo",
                Shape(
                    listOf(
                        Field(Identifier("id"), Primitive(Primitive.Type.String, false), false),
                        Field(Identifier("title"), Primitive(Primitive.Type.String, false), false),
                        Field(Identifier("completed"), Primitive(Primitive.Type.Boolean, false), false),
                        Field(Identifier("alert"), Reference.Custom("TodoAlert", false), false),
                    )
                )
            ),
            Type(
                "TodoAlert",
                Shape(
                    listOf(
                        Field(Identifier("code"), Primitive(Primitive.Type.String, false), false),
                        Field(Identifier("message"), Reference.Custom("TodoAlertMessage", false), false),
                    )
                )
            ),
            Type(
                "TodoAlertMessage",
                Shape(
                    listOf(
                        Field(Identifier("key"), Primitive(Primitive.Type.String, false), false),
                        Field(Identifier("value"), Primitive(Primitive.Type.String, false), false),
                    )
                )
            ),
            Type(
                "TodosnestedArray",
                Shape(
                    listOf(
                        Field(Identifier("id"), Primitive(Primitive.Type.String, false), false),
                        Field(Identifier("title"), Primitive(Primitive.Type.String, false), false),
                        Field(Identifier("nested"), Primitive(Primitive.Type.Boolean, false), false),
                    )
                )
            ),
            Type(
                "Error",
                Shape(
                    listOf(
                        Field(Identifier("code"), Primitive(Primitive.Type.String, false), false),
                        Field(Identifier("message"), Primitive(Primitive.Type.String, false), false),
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
