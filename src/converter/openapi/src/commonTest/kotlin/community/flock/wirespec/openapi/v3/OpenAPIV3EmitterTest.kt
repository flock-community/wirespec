package community.flock.wirespec.openapi.v3

import community.flock.kotlinx.openapi.bindings.OpenAPIV3
import community.flock.wirespec.openapi.common.Ast
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser.parse
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
        openapi shouldEqualJson expect
    }

    @Test
    fun petstoreParseEmitParse() {
        val path = Path("src/commonTest/resources/v3/petstore.json")
        val petstoreJson = SystemFileSystem.source(path).buffered().readString()

        val petstoreOpenAPi = OpenAPIV3.decodeFromString(petstoreJson)
        val petstoreAst = petstoreOpenAPi.parse().shouldNotBeNull()

        val petstoreConvertedOpenAPi = OpenAPIV3Emitter.emitOpenAPIObject(petstoreAst, null)

        val petstoreConvertedOpenAPiAst = petstoreConvertedOpenAPi.parse()

        petstoreConvertedOpenAPiAst shouldBe petstoreAst
    }
}
