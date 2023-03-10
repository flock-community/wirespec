package community.flock.wirespec.openapi

import arrow.core.orNull
import community.flock.kotlinx.openapi.bindings.OpenAPI
import community.flock.wirespec.compiler.core.Wirespec
import community.flock.wirespec.compiler.core.emit
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.parse.EndpointDefinition
import community.flock.wirespec.compiler.core.parse.Shape
import community.flock.wirespec.compiler.core.parse.Shape.Field
import community.flock.wirespec.compiler.core.parse.Shape.Field.Key
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value
import community.flock.wirespec.compiler.core.parse.Shape.Field.Value.Primitive
import community.flock.wirespec.compiler.core.parse.TypeDefinition
import community.flock.wirespec.compiler.utils.Logger
import kotlin.test.Test
import kotlin.test.assertEquals


expect object IO {
    fun readOpenApi(file: String): String
}

class MainTest {

    @Test
    fun start() {

        val json = IO.readOpenApi("todo.json")

        val openApi = OpenAPI.decodeFromString(json)

        val ast = OpenApiParser.parse(openApi)

        val expect = listOf(
            EndpointDefinition(
                EndpointDefinition.Name("TodosList"),
                EndpointDefinition.Method.GET,
                listOf(
                    EndpointDefinition.Segment.Literal("todos")
                ),
                listOf(
                    EndpointDefinition.Response("200", "application/json", Value.Custom("Todos", true)),
                    EndpointDefinition.Response("500", "application/json", Value.Custom("Error", false))
                )
            ),
            EndpointDefinition(
                EndpointDefinition.Name("TodosIdGET"),
                EndpointDefinition.Method.GET,
                listOf(
                    EndpointDefinition.Segment.Literal("todos"),
                    EndpointDefinition.Segment.Param("id", Primitive(Primitive.PrimitiveType.String))
                ),
                listOf(
                    EndpointDefinition.Response("200", "application/json", Value.Custom("Todo", false)),
                    EndpointDefinition.Response("500", "application/json", Value.Custom("Error", false))
                )
            ),
            TypeDefinition(
                TypeDefinition.Name("Todo"),
                Shape(
                    listOf(
                        Field(Key("id"), Primitive(Primitive.PrimitiveType.String), false),
                        Field(Key("title"), Primitive(Primitive.PrimitiveType.String), false),
                        Field(Key("completed"), Primitive(Primitive.PrimitiveType.Boolean), false),
                        Field(Key("alert"), Value.Custom("TodoAlert", false), false),
                    )
                )
            ),
            TypeDefinition(
                TypeDefinition.Name("TodoAlert"),
                Shape(
                    listOf(
                        Field(Key("code"), Primitive(Primitive.PrimitiveType.String), false),
                        Field(Key("message"), Value.Custom("TodoAlertMessage", false), false),
                    )
                )
            ),
            TypeDefinition(
                TypeDefinition.Name("TodoAlertMessage"),
                Shape(
                    listOf(
                        Field(Key("key"), Primitive(Primitive.PrimitiveType.String), false),
                        Field(Key("value"), Primitive(Primitive.PrimitiveType.String), false),
                    )
                )
            ),
            TypeDefinition(
                TypeDefinition.Name("Todos"),
                Value.Custom("Todo", true)
            ),
            TypeDefinition(
                TypeDefinition.Name("Error"),
                Shape(
                    listOf(
                        Field(Key("code"), Primitive(Primitive.PrimitiveType.String), false),
                        Field(Key("message"), Primitive(Primitive.PrimitiveType.String), false),
                    )
                )
            ),
        )

        val logger = object : Logger(true) {}
        val output = Wirespec.emit(ast)(logger)(KotlinEmitter())

        println("-------------")
        println(output.orNull()?.map { it.second }?.joinToString("\n"))
        println("-------------")

        assertEquals(expect, ast)
    }
}
