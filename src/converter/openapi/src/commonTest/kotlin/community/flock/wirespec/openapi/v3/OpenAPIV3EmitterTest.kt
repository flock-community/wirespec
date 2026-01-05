package community.flock.wirespec.openapi.v3

import arrow.core.nonEmptyListOf
import community.flock.kotlinx.openapi.bindings.OpenAPIV3
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.openapi.common.Ast
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser.parse
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class OpenAPIV3EmitterTest {

    val json = Json { prettyPrint = true }

    @Test
    fun astArray() {
        val res = OpenAPIV3Emitter.emitOpenAPIObject(Ast.array, null)
        val openapi = json.encodeToString(res)
        val expect = """
            {
                "openapi": "3.0.0",
                "info": {
                    "title": "Wirespec",
                    "version": "0.0.0"
                },
                "paths": {
                    "/array": {
                        "get": {
                            "operationId": "ArrayGET",
                            "parameters": [],
                            "requestBody": {
                                "content": {
                                    "application/json": {
                                        "schema": {
                                            "nullable": false,
                                            "type": "array",
                                            "items": {
                                                "nullable": false,
                                                "type": "array",
                                                "items": {
                                                    "${'$'}ref": "#/components/schemas/MessageArray"
                                                }
                                            }
                                        }
                                    }
                                },
                                "required": true
                            },
                            "responses": {
                                "200": {
                                    "description": "Ok",
                                    "headers": {},
                                    "content": {
                                        "application/json": {
                                            "schema": {
                                                "nullable": false,
                                                "type": "array",
                                                "items": {
                                                    "${'$'}ref": "#/components/schemas/ArrayGET200ResponseBody"
                                                }
                                            }
                                        }
                                    }
                                },
                                "201": {
                                    "description": "Created",
                                    "headers": {},
                                    "content": {
                                        "application/json": {
                                            "schema": {
                                                "nullable": false,
                                                "type": "array",
                                                "items": {
                                                    "nullable": false,
                                                    "type": "array",
                                                    "items": {
                                                        "${'$'}ref": "#/components/schemas/MessageArray"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                "202": {
                                    "description": "Created",
                                    "headers": {},
                                    "content": {
                                        "application/json": {
                                            "schema": {
                                                "nullable": false,
                                                "type": "array",
                                                "items": {
                                                    "type": "string"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                "components": {
                    "schemas": {
                        "ArrayGET200ResponseBody": {
                            "properties": {
                                "code": {
                                    "type": "number",
                                    "format": "double"
                                },
                                "text": {
                                    "type": "string"
                                }
                            }
                        },
                        "MessageArray": {
                            "properties": {
                                "code": {
                                    "type": "number",
                                    "format": "double"
                                },
                                "text": {
                                    "type": "string"
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        openapi shouldEqualJson expect
    }

    @Test
    fun petstoreParseEmitParse() {
        val path = Path("src/commonTest/resources/v3/petstore.json")
        val petstoreJson = SystemFileSystem.source(path).buffered().readString()

        val petstoreOpenAPi = OpenAPIV3.decodeFromString(petstoreJson)
        val petstoreAst = petstoreOpenAPi.parse().shouldNotBeNull()

        val petstoreConvertedOpenAPi = OpenAPIV3Emitter.emitOpenAPIObject(petstoreAst, null)

        val petstoreConvertedOpenAPiAst = petstoreConvertedOpenAPi.parse() ?: error("Failed to parse converted OpenAPI object")

        (petstoreAst zip petstoreConvertedOpenAPiAst)
            .forEach { (actual, expected) ->
                println("${actual::class.simpleName} ${actual.identifier.value}")
                actual shouldBe expected
            }
    }

    @Test
    fun descriptionAnnotation() {
        val source = """
            @Description("Todo object")
            type Todo {
              @Description("id field") id: String,
              @Description("done field") done: Boolean,
              @Description("prio field") prio: Integer
            }

            @Description("Error object")
            type Error {
               @Description("reason field") reason: String
            }

            @Description("Get all todos")
            endpoint GetTodos GET /todos -> {
                @Description("GetTodos 200 response")
                200 -> Todo[]
            }
        """.trimIndent()

        val ast = parser(source).shouldBeRight()
        val openapi = OpenAPIV3Emitter.emit(ast, noLogger).first().result

        val expect = """
            {
                "openapi": "3.0.0",
                "info": {
                    "title": "Wirespec",
                    "version": "0.0.0"
                },
                "paths": {
                    "/todos": {
                        "get": {
                            "operationId": "GetTodos",
                            "description": "Get all todos",
                            "parameters": [],
                            "responses": {
                                "200": {
                                    "description": "GetTodos 200 response",
                                    "headers": {},
                                    "content": {
                                        "application/json": {
                                            "schema": {
                                                "nullable": false,
                                                "type": "array",
                                                "items": {
                                                    "${'$'}ref": "#/components/schemas/Todo"
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                "components": {
                    "schemas": {
                        "Todo": {
                            "description": "Todo object",
                            "properties": {
                                "id": {
                                    "type": "string",
                                    "description": "id field"
                                },
                                "done": {
                                    "type": "boolean",
                                    "description": "done field"
                                },
                                "prio": {
                                    "type": "integer",
                                    "format": "int64",
                                    "description": "prio field"
                                }
                            },
                            "required": [
                                "id",
                                "done",
                                "prio"
                            ]
                        },
                        "Error": {
                            "description": "Error object",
                            "properties": {
                                "reason": {
                                    "type": "string",
                                    "description": "reason field"
                                }
                            },
                            "required": [
                                "reason"
                            ]
                        }
                    }
                }
            }
        """.trimIndent()

        openapi shouldEqualJson expect
    }

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source))).map { AST(it.modules) }
}
