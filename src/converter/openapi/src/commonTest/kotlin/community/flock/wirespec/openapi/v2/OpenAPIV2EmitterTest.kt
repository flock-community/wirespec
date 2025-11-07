package community.flock.wirespec.openapi.v2

import arrow.core.nonEmptyListOf
import community.flock.kotlinx.openapi.bindings.OpenAPIV2
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser.parse
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class OpenAPIV2EmitterTest {

    val json = Json { prettyPrint = true }

    @Test
    fun petstoreParseEmitParse() {
        val path = Path("src/commonTest/resources/v2/petstore.json")
        val petstoreJson = SystemFileSystem.source(path).buffered().readString()

        val petstoreOpenAPi = OpenAPIV2.decodeFromString(petstoreJson)
        val petstoreAst = petstoreOpenAPi.parse().shouldNotBeNull()

        val petstoreConvertedOpenAPI = OpenAPIV2Emitter.emitSwaggerObject(petstoreAst)
        val petstoreConvertedOpenAPiAst = petstoreConvertedOpenAPI.parse().shouldNotBeNull()

        val actual = petstoreAst.toList()
            .sortedBy { it.identifier.value }
            .joinToString("\n")

        val expected = petstoreConvertedOpenAPiAst
            .sortedBy { it.identifier.value }
            .joinToString("\n")

        actual shouldBe expected
    }

    @Test
    fun descriptionAnnotation() {
        val source =
            // language=ws
            """
            |@Description("Todo object")
            |type Todo {
            |  @Description("id field") id: String,
            |  @Description("done field") done: Boolean,
            |  @Description("prio field") prio: Integer
            |}
            |
            |@Description("Error object")
            |type Error {
            |   @Description("reason field") reason: String
            |}
            |
            |@Description("Get all todos")
            |endpoint GetTodos GET /todos -> {
            |    @Description("GetTodos 200 response")
            |    200 -> Todo[]
            |}
            """.trimMargin()

        val ast = parser(source).shouldBeRight()
        val openapi = OpenAPIV2Emitter.emit(ast, noLogger).first().result

        val expect =
            """
            |{
            |    "swagger": "2.0",
            |    "consumes": [
            |        "application/json"
            |    ],
            |    "produces": [
            |        "application/json"
            |    ],
            |    "definitions": {
            |        "Todo": {
            |            "description": "Todo object",
            |            "required": [
            |                "id",
            |                "done",
            |                "prio"
            |            ],
            |            "properties": {
            |                "id": {
            |                    "type": "string",
            |                    "description": "id field"
            |                },
            |                "done": {
            |                    "type": "boolean",
            |                    "description": "done field"
            |                },
            |                "prio": {
            |                    "type": "integer",
            |                    "description": "prio field",
            |                    "format": "int64"
            |                }
            |            }
            |        },
            |        "Error": {
            |            "description": "Error object",
            |            "required": [
            |                "reason"
            |            ],
            |            "properties": {
            |                "reason": {
            |                    "type": "string",
            |                    "description": "reason field"
            |                }
            |            }
            |        }
            |    },
            |    "info": {
            |        "title": "Wirespec",
            |        "version": "0.0.0"
            |    },
            |    "paths": {
            |        "/todos": {
            |            "parameters": [],
            |            "get": {
            |                "produces": [
            |                    "application/json"
            |                ],
            |                "parameters": [],
            |                "responses": {
            |                    "200": {
            |                        "schema": {
            |                            "type": "array",
            |                            "items": {
            |                                "${'$'}ref": "#/definitions/Todo"
            |                            }
            |                        },
            |                        "description": "GetTodos 200 response",
            |                        "headers": {}
            |                    }
            |                },
            |                "description": "Get all todos",
            |                "operationId": "GetTodos"
            |            }
            |        }
            |    }
            |}
            """.trimMargin()

        openapi shouldEqualJson expect
    }

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source))).map { AST(it.modules) }
}
