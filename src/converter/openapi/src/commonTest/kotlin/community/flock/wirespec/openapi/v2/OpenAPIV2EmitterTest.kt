package community.flock.wirespec.openapi.v2

import community.flock.kotlinx.openapi.bindings.OpenAPIV2
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.test.compile
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

        val petstoreConvertedOpenAPI = OpenAPIV2Emitter.emitSwaggerObject(petstoreAst, noLogger)
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

        val result = compile(source)() { OpenAPIV2Emitter }

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

        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileFullEndpointTest() {
        val result = CompileFullEndpointTest.compiler { OpenAPIV2Emitter }
        val expect =
            // language=json
            """
            |{
            |  "swagger": "2.0",
            |  "consumes": [
            |    "application/json"
            |  ],
            |  "produces": [
            |    "application/json"
            |  ],
            |  "definitions": {
            |    "PotentialTodoDto": {
            |      "required": [
            |        "name",
            |        "done"
            |      ],
            |      "properties": {
            |        "name": {
            |          "type": "string"
            |        },
            |        "done": {
            |          "type": "boolean"
            |        }
            |      }
            |    },
            |    "Token": {
            |      "required": [
            |        "iss"
            |      ],
            |      "properties": {
            |        "iss": {
            |          "type": "string"
            |        }
            |      }
            |    },
            |    "TodoDto": {
            |      "required": [
            |        "id",
            |        "name",
            |        "done"
            |      ],
            |      "properties": {
            |        "id": {
            |          "type": "string"
            |        },
            |        "name": {
            |          "type": "string"
            |        },
            |        "done": {
            |          "type": "boolean"
            |        }
            |      }
            |    },
            |    "Error": {
            |      "required": [
            |        "code",
            |        "description"
            |      ],
            |      "properties": {
            |        "code": {
            |          "type": "integer",
            |          "format": "int64"
            |        },
            |        "description": {
            |          "type": "string"
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
            |          "type": "string",
            |          "in": "path",
            |          "name": "id"
            |        }
            |      ],
            |      "put": {
            |        "consumes": [
            |          "application/json"
            |        ],
            |        "produces": [
            |          "application/json"
            |        ],
            |        "parameters": [
            |          {
            |            "required": true,
            |            "in": "body",
            |            "schema": {
            |              "${'$'}ref": "#/definitions/PotentialTodoDto"
            |            },
            |            "name": "RequestBody"
            |          },
            |          {
            |            "required": true,
            |            "type": "boolean",
            |            "in": "query",
            |            "name": "done"
            |          },
            |          {
            |            "required": false,
            |            "type": "string",
            |            "in": "query",
            |            "name": "name"
            |          },
            |          {
            |            "required": true,
            |            "type": "object",
            |            "in": "header",
            |            "name": "token"
            |          },
            |          {
            |            "required": false,
            |            "type": "object",
            |            "in": "header",
            |            "name": "Refresh-Token"
            |          }
            |        ],
            |        "responses": {
            |          "200": {
            |            "schema": {
            |              "${'$'}ref": "#/definitions/TodoDto"
            |            },
            |            "description": "PutTodo 200 response",
            |            "headers": {}
            |          },
            |          "201": {
            |            "schema": {
            |              "${'$'}ref": "#/definitions/TodoDto"
            |            },
            |            "description": "PutTodo 201 response",
            |            "headers": {
            |              "token": {
            |                "type": "object"
            |              },
            |              "refreshToken": {
            |                "type": "object"
            |              }
            |            }
            |          },
            |          "500": {
            |            "schema": {
            |              "${'$'}ref": "#/definitions/Error"
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
        val result = CompileMinimalEndpointTest.compiler { OpenAPIV2Emitter }
        val expect =
            // language=json
            """
                |{
                |  "swagger": "2.0",
                |  "info": {
                |    "title": "Wirespec",
                |    "version": "0.0.0"
                |  },
                |  "consumes": ["application/json"],
                |  "produces": ["application/json"],
                |  "paths": {
                |    "/todos": {
                |      "parameters": [],
                |      "get": {
                |        "operationId": "GetTodos",
                |        "produces": ["application/json"],
                |        "parameters": [],
                |        "responses": {
                |          "200": {
                |            "description": "GetTodos 200 response",
                |            "headers": {},
                |            "schema": {
                |              "type": "array",
                |              "items": {
                |                "${'$'}ref": "#/definitions/TodoDto"
                |              }
                |            }
                |          }
                |        }
                |      }
                |    }
                |  },
                |  "definitions": {
                |    "TodoDto": {
                |      "required": ["description"],
                |      "properties": {
                |        "description": {
                |          "type": "string"
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
        val result = CompileChannelTest.compiler { OpenAPIV2Emitter }
        val expect =
            // language=json
            """
            |{
            |  "swagger": "2.0",
            |  "consumes": [
            |    "application/json"
            |  ],
            |  "produces": [
            |    "application/json"
            |  ],
            |  "definitions": {},
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
        val result = CompileEnumTest.compiler { OpenAPIV2Emitter }
        val expect =
            // language=json
            """
                |{
                |  "swagger": "2.0",
                |  "info": {
                |    "title": "Wirespec",
                |    "version": "0.0.0"
                |  },
                |  "consumes": ["application/json"],
                |  "produces": ["application/json"],
                |  "paths": {},
                |  "definitions": {
                |    "MyAwesomeEnum": {
                |      "type": "string",
                |      "enum": ["ONE", "Two", "THREE_MORE", "UnitedKingdom"]
                |    }
                |  }
                |}
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileRefinedTest() {
        val result = CompileRefinedTest.compiler { OpenAPIV2Emitter }
        val expect =
            // language=json
            """
                |{
                |  "swagger": "2.0",
                |  "info": {
                |    "title": "Wirespec",
                |    "version": "0.0.0"
                |  },
                |  "consumes": ["application/json"],
                |  "produces": ["application/json"],
                |  "paths": {},
                |  "definitions": {
                |    "TodoId": {
                |      "type": "string",
                |      "pattern": "/^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$/g"
                |    },
                |    "TodoNoRegex": {
                |      "type": "string"
                |    },
                |    "TestInt": {
                |      "type": "integer",
                |      "format": "int32"
                |    },
                |    "TestInt0": {
                |      "type": "integer",
                |      "format": "int32"
                |    },
                |    "TestInt1": {
                |      "type": "integer",
                |      "format": "int32",
                |      "minimum": 0.0
                |    },
                |    "TestInt2": {
                |      "type": "integer",
                |      "format": "int32",
                |      "minimum": 3.0,
                |      "maximum": 1.0
                |    },
                |    "TestNum": {
                |      "type": "number",
                |      "format": "float"
                |    },
                |    "TestNum0": {
                |      "type": "number",
                |      "format": "float"
                |    },
                |    "TestNum1": {
                |      "type": "number",
                |      "format": "float",
                |      "maximum": 0.5
                |    },
                |    "TestNum2": {
                |      "type": "number",
                |      "format": "float",
                |      "minimum": -0.2,
                |      "maximum": 0.5
                |    }
                |  }
                |}
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileUnionTest() {
        val result = CompileUnionTest.compiler { OpenAPIV2Emitter }
        val expect =
            // language=json
            """
            |{
            |  "swagger": "2.0",
            |  "consumes": [
            |    "application/json"
            |  ],
            |  "produces": [
            |    "application/json"
            |  ],
            |  "definitions": {
            |    "UserAccountPassword": {
            |      "required": [
            |        "username",
            |        "password"
            |      ],
            |      "properties": {
            |        "username": {
            |          "type": "string"
            |        },
            |        "password": {
            |          "type": "string"
            |        }
            |      }
            |    },
            |    "UserAccountToken": {
            |      "required": [
            |        "token"
            |      ],
            |      "properties": {
            |        "token": {
            |          "type": "string"
            |        }
            |      }
            |    },
            |    "User": {
            |      "required": [
            |        "username",
            |        "account"
            |      ],
            |      "properties": {
            |        "username": {
            |          "type": "string"
            |        },
            |        "account": {
            |          "${'$'}ref": "#/definitions/UserAccount"
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

    @Test
    fun compileTypeTest() {
        val result = CompileTypeTest.compiler { OpenAPIV2Emitter }
        val expect =
            // language=json
            """
            |{
            |  "swagger": "2.0",
            |  "consumes": [
            |    "application/json"
            |  ],
            |  "produces": [
            |    "application/json"
            |  ],
            |  "definitions": {
            |    "Request": {
            |      "required": [
            |        "type",
            |        "url",
            |        "params",
            |        "headers"
            |      ],
            |      "properties": {
            |        "type": {
            |          "type": "string"
            |        },
            |        "url": {
            |          "type": "string"
            |        },
            |        "BODY_TYPE": {
            |          "type": "string"
            |        },
            |        "params": {
            |          "type": "array",
            |          "items": {
            |            "type": "string"
            |          }
            |        },
            |        "headers": {
            |          "type": "object",
            |          "items": {
            |            "type": "string"
            |          }
            |        },
            |        "body": {
            |          "type": "object",
            |          "items": {
            |            "type": "array",
            |            "items": {
            |              "type": "string"
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
