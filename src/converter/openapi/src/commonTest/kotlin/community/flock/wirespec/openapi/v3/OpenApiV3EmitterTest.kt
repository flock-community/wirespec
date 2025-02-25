package community.flock.wirespec.openapi.v3

import community.flock.kotlinx.openapi.bindings.v3.OpenAPI
import community.flock.wirespec.openapi.common.Ast
import community.flock.wirespec.openapi.v3.OpenApiV3Parser.parse
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiV3EmitterTest {

    val json = Json { prettyPrint = true }

    @Test
    fun astArray() {
        val res = OpenApiV3Emitter.emitOpenAPIObject(Ast.array, null)
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
                                    "description": "ArrayGET 200 response",
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
                                    "description": "ArrayGET 201 response",
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
                                    "description": "ArrayGET 202 response",
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
        assertEquals(expect, openapi)
    }

    @Test
    fun petstoreParseEmitParse() {
        val path = Path("src/commonTest/resources/v3/petstore.json")
        val petstoreJson = SystemFileSystem.source(path).buffered().readString()

        val petstoreOpenAPi = OpenAPI.decodeFromString(petstoreJson)
        val petstoreAst = petstoreOpenAPi.parse()

        val petstoreConvertedOpenAPi = OpenApiV3Emitter.emitOpenAPIObject(petstoreAst, null)

//        println(json.encodeToString(petstoreConvertedOpenAPi))
        val petstoreConvertedOpenAPiAst = petstoreConvertedOpenAPi.parse()

        assertEquals(petstoreAst, petstoreConvertedOpenAPiAst)
    }
}
