package community.flock.wirespec.openapi.v3

import community.flock.kotlinx.openapi.bindings.OpenAPIV3
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.test.compile
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
        val res = OpenAPIV3Emitter.emitOpenAPIObject(Ast.array, null, noLogger)
        val openapi = json.encodeToString(res)
        val expect =
            """
            |{
            |    "openapi": "3.0.0",
            |    "info": {
            |        "title": "Wirespec",
            |        "version": "0.0.0"
            |    },
            |    "paths": {
            |        "/array": {
            |            "get": {
            |                "operationId": "ArrayGET",
            |                "parameters": [],
            |                "requestBody": {
            |                    "content": {
            |                        "application/json": {
            |                            "schema": {
            |                                "nullable": false,
            |                                "type": "array",
            |                                "items": {
            |                                    "nullable": false,
            |                                    "type": "array",
            |                                    "items": {
            |                                        "${'$'}ref": "#/components/schemas/MessageArray"
            |                                    }
            |                                }
            |                            }
            |                        }
            |                    },
            |                    "required": true
            |                },
            |                "responses": {
            |                    "200": {
            |                        "description": "Ok",
            |                        "headers": {},
            |                        "content": {
            |                            "application/json": {
            |                                "schema": {
            |                                    "nullable": false,
            |                                    "type": "array",
            |                                    "items": {
            |                                        "${'$'}ref": "#/components/schemas/ArrayGET200ResponseBody"
            |                                    }
            |                                }
            |                            }
            |                        }
            |                    },
            |                    "201": {
            |                        "description": "Created",
            |                        "headers": {},
            |                        "content": {
            |                            "application/json": {
            |                                "schema": {
            |                                    "nullable": false,
            |                                    "type": "array",
            |                                    "items": {
            |                                        "nullable": false,
            |                                        "type": "array",
            |                                        "items": {
            |                                            "${'$'}ref": "#/components/schemas/MessageArray"
            |                                        }
            |                                    }
            |                                }
            |                            }
            |                        }
            |                    },
            |                    "202": {
            |                        "description": "Created",
            |                        "headers": {},
            |                        "content": {
            |                            "application/json": {
            |                                "schema": {
            |                                    "nullable": false,
            |                                    "type": "array",
            |                                    "items": {
            |                                        "type": "string"
            |                                    }
            |                                }
            |                            }
            |                        }
            |                    }
            |                }
            |            }
            |        }
            |    },
            |    "components": {
            |        "schemas": {
            |            "ArrayGET200ResponseBody": {
            |                "properties": {
            |                    "code": {
            |                        "type": "number",
            |                        "format": "double"
            |                    },
            |                    "text": {
            |                        "type": "string"
            |                    }
            |                }
            |            },
            |            "MessageArray": {
            |                "properties": {
            |                    "code": {
            |                        "type": "number",
            |                        "format": "double"
            |                    },
            |                    "text": {
            |                        "type": "string"
            |                    }
            |                }
            |            }
            |        }
            |    }
            |}
            """.trimMargin()
        openapi shouldEqualJson expect
    }

    @Test
    fun petstoreParseEmitParse() {
        val path = Path("src/commonTest/resources/v3/petstore.json")
        val petstoreJson = SystemFileSystem.source(path).buffered().readString()

        val petstoreOpenAPi = OpenAPIV3.decodeFromString(petstoreJson)
        val petstoreAst = petstoreOpenAPi.parse().shouldNotBeNull()

        val petstoreConvertedOpenAPi = OpenAPIV3Emitter.emitOpenAPIObject(petstoreAst, null, noLogger)

        val petstoreConvertedOpenAPiAst = petstoreConvertedOpenAPi.parse()

        (petstoreAst zip petstoreConvertedOpenAPiAst)
            .forEach { (actual, expected) ->
                println("${actual::class.simpleName} ${actual.identifier.value}")
                actual shouldBe expected
            }
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

        val result = compile(source).invoke { OpenAPIV3Emitter }

        val expect =
            // language=json
            """
            |{
            |    "openapi": "3.0.0",
            |    "info": {
            |        "title": "Wirespec",
            |        "version": "0.0.0"
            |    },
            |    "paths": {
            |        "/todos": {
            |            "get": {
            |                "operationId": "GetTodos",
            |                "description": "Get all todos",
            |                "parameters": [],
            |                "responses": {
            |                    "200": {
            |                        "description": "GetTodos 200 response",
            |                        "headers": {},
            |                        "content": {
            |                            "application/json": {
            |                                "schema": {
            |                                    "nullable": false,
            |                                    "type": "array",
            |                                    "items": {
            |                                        "${'$'}ref": "#/components/schemas/Todo"
            |                                    }
            |                                }
            |                            }
            |                        }
            |                    }
            |                }
            |            }
            |        }
            |    },
            |    "components": {
            |        "schemas": {
            |            "Todo": {
            |                "description": "Todo object",
            |                "properties": {
            |                    "id": {
            |                        "type": "string",
            |                        "description": "id field"
            |                    },
            |                    "done": {
            |                        "type": "boolean",
            |                        "description": "done field"
            |                    },
            |                    "prio": {
            |                        "type": "integer",
            |                        "format": "int64",
            |                        "description": "prio field"
            |                    }
            |                },
            |                "required": [
            |                    "id",
            |                    "done",
            |                    "prio"
            |                ]
            |            },
            |            "Error": {
            |                "description": "Error object",
            |                "properties": {
            |                    "reason": {
            |                        "type": "string",
            |                        "description": "reason field"
            |                    }
            |                },
            |                "required": [
            |                    "reason"
            |                ]
            |            }
            |        }
            |    }
            |}
            """.trimMargin()

        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileFullEndpointTest() {
        val result = CompileFullEndpointTest.compiler { OpenAPIV3Emitter }
        val expect =
            // language=json
            """
                |{
                |  "openapi": "3.0.0",
                |  "components": {
                |    "schemas": {
                |      "PotentialTodoDto": {
                |        "required": [
                |          "name",
                |          "done"
                |        ],
                |        "properties": {
                |          "name": {
                |            "type": "string"
                |          },
                |          "done": {
                |            "type": "boolean"
                |          }
                |        }
                |      },
                |      "Token": {
                |        "required": [
                |          "iss"
                |        ],
                |        "properties": {
                |          "iss": {
                |            "type": "string"
                |          }
                |        }
                |      },
                |      "TodoDto": {
                |        "required": [
                |          "id",
                |          "name",
                |          "done"
                |        ],
                |        "properties": {
                |          "id": {
                |            "type": "string"
                |          },
                |          "name": {
                |            "type": "string"
                |          },
                |          "done": {
                |            "type": "boolean"
                |          }
                |        }
                |      },
                |      "Error": {
                |        "required": [
                |          "code",
                |          "description"
                |        ],
                |        "properties": {
                |          "code": {
                |            "type": "integer",
                |            "format": "int64"
                |          },
                |          "description": {
                |            "type": "string"
                |          }
                |        }
                |      }
                |    }
                |  },
                |  "info": {
                |    "title": "Wirespec",
                |    "version": "0.0.0"
                |  },
                |  "paths": {
                |    "/todos/{id}": {
                |      "parameters": [
                |        {
                |          "required": true,
                |          "in": "path",
                |          "schema": {
                |            "type": "string"
                |          },
                |          "name": "id"
                |        }
                |      ],
                |      "put": {
                |        "parameters": [
                |          {
                |            "required": true,
                |            "in": "path",
                |            "schema": {
                |              "type": "string"
                |            },
                |            "name": "id"
                |          },
                |          {
                |            "required": true,
                |            "in": "query",
                |            "schema": {
                |              "type": "boolean"
                |            },
                |            "name": "done"
                |          },
                |          {
                |            "required": false,
                |            "in": "query",
                |            "schema": {
                |              "type": "string"
                |            },
                |            "name": "name"
                |          },
                |          {
                |            "required": true,
                |            "in": "header",
                |            "schema": {
                |              "${'$'}ref": "#/components/schemas/Token"
                |            },
                |            "name": "token"
                |          },
                |          {
                |            "required": false,
                |            "in": "header",
                |            "schema": {
                |              "${'$'}ref": "#/components/schemas/Token"
                |            },
                |            "name": "Refresh-Token"
                |          }
                |        ],
                |        "requestBody": {
                |          "content": {
                |            "application/json": {
                |              "schema": {
                |                "${'$'}ref": "#/components/schemas/PotentialTodoDto"
                |              }
                |            }
                |          },
                |          "required": true
                |        },
                |        "responses": {
                |          "200": {
                |            "content": {
                |              "application/json": {
                |                "schema": {
                |                  "${'$'}ref": "#/components/schemas/TodoDto"
                |                }
                |              }
                |            },
                |            "description": "PutTodo 200 response",
                |            "headers": {}
                |          },
                |          "201": {
                |            "content": {
                |              "application/json": {
                |                "schema": {
                |                  "${'$'}ref": "#/components/schemas/TodoDto"
                |                }
                |              }
                |            },
                |            "description": "PutTodo 201 response",
                |            "headers": {
                |              "token": {
                |                "${'$'}ref": "#/components/headers/Token"
                |              },
                |              "refreshToken": {
                |                "${'$'}ref": "#/components/headers/Token"
                |              }
                |            }
                |          },
                |          "500": {
                |            "content": {
                |              "application/json": {
                |                "schema": {
                |                  "${'$'}ref": "#/components/schemas/Error"
                |                }
                |              }
                |            },
                |            "description": "PutTodo 500 response",
                |            "headers": {}
                |          }
                |        },
                |        "operationId": "PutTodo"
                |      }
                |    }
                |  }
                |}
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileMinimalEndpointTest() {
        val result = CompileMinimalEndpointTest.compiler { OpenAPIV3Emitter }
        val expect =
            // language=json
            """
                |{
                |  "openapi": "3.0.0",
                |  "info": {
                |    "title": "Wirespec",
                |    "version": "0.0.0"
                |  },
                |  "paths": {
                |    "/todos": {
                |      "get": {
                |        "operationId": "GetTodos",
                |        "parameters": [],
                |        "responses": {
                |          "200": {
                |            "description": "GetTodos 200 response",
                |            "headers": {},
                |            "content": {
                |              "application/json": {
                |                "schema": {
                |                  "nullable": false,
                |                  "type": "array",
                |                  "items": {
                |                    "${'$'}ref": "#/components/schemas/TodoDto"
                |                  }
                |                }
                |              }
                |            }
                |          }
                |        }
                |      }
                |    }
                |  },
                |  "components": {
                |    "schemas": {
                |      "TodoDto": {
                |        "required": [
                |          "description"
                |        ],
                |        "properties": {
                |          "description": {
                |            "type": "string"
                |          }
                |        }
                |      }
                |    }
                |  }
                |}
            """.trimMargin()

        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileChannelTest() {
        val result = CompileChannelTest.compiler { OpenAPIV3Emitter }
        val expect =
            // language=json
            """
            |{
            |  "openapi": "3.0.0",
            |  "components": {
            |    "schemas": {}
            |  },
            |  "info": {
            |    "title": "Wirespec",
            |    "version": "0.0.0"
            |  },
            |  "paths": {}
            |}
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileEnumTest() {
        val result = CompileEnumTest.compiler { OpenAPIV3Emitter }
        val expect =
            // language=json
            """
                |{
                |  "openapi": "3.0.0",
                |  "info": {
                |    "title": "Wirespec",
                |    "version": "0.0.0"
                |  },
                |  "paths": {},
                |  "components": {
                |    "schemas": {
                |      "MyAwesomeEnum": {
                |        "type": "string",
                |        "enum": ["ONE", "Two", "THREE_MORE", "UnitedKingdom"]
                |      }
                |    }
                |  }
                |}
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileNegativeEnumTest() {
        val result = CompileEnumTest.negativeCompiler { OpenAPIV3Emitter }
        val expect =
            // language=json
            """
                |{
                |  "openapi": "3.0.0",
                |  "info": {
                |    "title": "Wirespec",
                |    "version": "0.0.0"
                |  },
                |  "paths": {},
                |  "components": {
                |    "schemas": {
                |      "InnerErrorCode": {
                |        "type": "string",
                |        "enum": ["0", "1", "-1", "2", "-999"]
                |      }
                |    }
                |  }
                |}
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileRefinedTest() {
        val result = CompileRefinedTest.compiler { OpenAPIV3Emitter }
        val expect =
            // language=json
            """
            |{
            |  "openapi": "3.0.0",
            |  "components": {
            |    "schemas": {
            |      "TodoId": {
            |        "type": "string",
            |        "pattern": "/^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$/g"
            |      },
            |      "TodoNoRegex": {
            |        "type": "string"
            |      },
            |      "TestInt": {
            |        "type": "integer",
            |        "format": "int32"
            |      },
            |      "TestInt0": {
            |        "type": "integer",
            |        "format": "int32"
            |      },
            |      "TestInt1": {
            |        "type": "integer",
            |        "format": "int32",
            |        "minimum": 0.0
            |      },
            |      "TestInt2": {
            |        "type": "integer",
            |        "format": "int32",
            |        "maximum": 1.0,
            |        "minimum": 3.0
            |      },
            |      "TestNum": {
            |        "type": "number",
            |        "format": "float"
            |      },
            |      "TestNum0": {
            |        "type": "number",
            |        "format": "float"
            |      },
            |      "TestNum1": {
            |        "type": "number",
            |        "format": "float",
            |        "maximum": 0.5
            |      },
            |      "TestNum2": {
            |        "type": "number",
            |        "format": "float",
            |        "maximum": 0.5,
            |        "minimum": -0.2
            |      }
            |    }
            |  },
            |  "info": {
            |    "title": "Wirespec",
            |    "version": "0.0.0"
            |  },
            |  "paths": {}
            |}
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileUnionTest() {
        val result = CompileUnionTest.compiler { OpenAPIV3Emitter }
        val expect =
            // language=json
            """
                |{
                |  "openapi": "3.0.0",
                |  "info": {
                |    "title": "Wirespec",
                |    "version": "0.0.0"
                |  },
                |  "paths": {},
                |  "components": {
                |    "schemas": {
                |      "UserAccount": {
                |        "oneOf": [
                |          {"${'$'}ref": "#/components/schemas/UserAccountPassword"},
                |          {"${'$'}ref": "#/components/schemas/UserAccountToken"}
                |        ],
                |        "type": "string"
                |      },
                |      "UserAccountPassword": {
                |        "required": ["username", "password"],
                |        "properties": {
                |          "username": {
                |            "type": "string"
                |          },
                |          "password": {
                |            "type": "string"
                |          }
                |        }
                |      },
                |      "UserAccountToken": {
                |        "required": ["token"],
                |        "properties": {
                |          "token": {
                |            "type": "string"
                |          }
                |        }
                |      },
                |      "User": {
                |        "required": ["username", "account"],
                |        "properties": {
                |          "username": {
                |            "type": "string"
                |          },
                |          "account": {
                |            "${'$'}ref": "#/components/schemas/UserAccount"
                |          }
                |        }
                |      }
                |    }
                |  }
                |}
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileTypeTest() {
        val result = CompileTypeTest.compiler { OpenAPIV3Emitter }
        val expect =
            // language=json
            """
                |{
                |  "openapi": "3.0.0",
                |  "components": {
                |    "schemas": {
                |      "Request": {
                |        "required": [
                |          "type",
                |          "url",
                |          "params",
                |          "headers"
                |        ],
                |        "properties": {
                |          "type": {
                |            "type": "string"
                |          },
                |          "url": {
                |            "type": "string"
                |          },
                |          "BODY_TYPE": {
                |            "type": "string"
                |          },
                |          "params": {
                |            "nullable": false,
                |            "type": "array",
                |            "items": {
                |              "type": "string"
                |            }
                |          },
                |          "headers": {
                |            "nullable": false,
                |            "type": "object",
                |            "additionalProperties": {
                |              "type": "string"
                |            }
                |          },
                |          "body": {
                |            "nullable": true,
                |            "type": "object",
                |            "additionalProperties": {
                |              "nullable": true,
                |              "type": "array",
                |              "items": {
                |                "type": "string"
                |              }
                |            }
                |          }
                |        }
                |      }
                |    }
                |  },
                |  "info": {
                |    "title": "Wirespec",
                |    "version": "0.0.0"
                |  },
                |  "paths": {}
                |}
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }
}
