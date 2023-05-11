package community.flock.wirespec.openapi

import community.flock.kotlinx.openapi.bindings.OpenAPI
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Type.Shape
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Identifier
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference
import community.flock.wirespec.compiler.core.parse.Type.Shape.Field.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.Type
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
                    Endpoint.Response("200", "application/json", Reference.Custom("Todos", true)),
                    Endpoint.Response("500", "application/json", Reference.Custom("Error", false))
                )
            ),
            Endpoint(
                "TodosIdGET",
                Endpoint.Method.GET,
                listOf(
                    Endpoint.Segment.Literal("todos"),
                    Endpoint.Segment.Param("id", Primitive(Primitive.Type.String, false))
                ),
                listOf(
                    Endpoint.Response("200", "application/json", Reference.Custom("Todo", false)),
                    Endpoint.Response("500", "application/json", Reference.Custom("Error", false))
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
