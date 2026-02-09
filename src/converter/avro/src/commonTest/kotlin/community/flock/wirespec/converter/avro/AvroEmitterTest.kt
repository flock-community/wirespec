package community.flock.wirespec.converter.avro

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.json.shouldEqualJson
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Ignore
import kotlin.test.Test

class AvroEmitterTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source))).getOrNull() ?: error("Parsing failed.")

    @Test
    fun testTodoWs() {
        val path = Path("src/commonTest/resources/todo.ws")
        val text = SystemFileSystem.source(path).buffered().readString()

        val ast = parse(text)
        val actual = AvroEmitter.emit(ast.modules.first()).let { json.encodeToString(it) }
        val expected =
            // language=json
            """
            |[
            |    {
            |        "type": "enum",
            |        "name": "Status",
            |        "symbols": [
            |            "PUBLIC",
            |            "PRIVATE"
            |        ]
            |    },
            |    {
            |        "type": "record",
            |        "name": "Left",
            |        "fields": [
            |            {
            |                "name": "left",
            |                "type": "string"
            |            }
            |        ]
            |    },
            |    {
            |        "type": "record",
            |        "name": "Right",
            |        "fields": [
            |            {
            |                "name": "right",
            |                "type": "string"
            |            }
            |        ]
            |    },
            |    {
            |        "name": "Either",
            |        "type": [
            |            "Left",
            |            "Right"
            |        ]
            |    },
            |    {
            |        "type": "record",
            |        "name": "Todo",
            |        "fields": [
            |            {
            |                "name": "id",
            |                "type": "string"
            |            },
            |            {
            |                "name": "name",
            |                "type": [
            |                    "null",
            |                    "string"
            |                ]
            |            },
            |            {
            |                "name": "done",
            |                "type": "boolean"
            |            },
            |            {
            |                "name": "tags",
            |                "type": {
            |                    "type": "array",
            |                    "items": "string"
            |                }
            |            },
            |            {
            |                "name": "status",
            |                "type": "Status"
            |            },
            |            {
            |                "name": "either",
            |                "type": "Either"
            |            }
            |        ]
            |    }
            |]
            """.trimMargin()

        actual shouldEqualJson expected
    }

    @Test
    @Ignore
    fun testSimple() {
        val path = Path("src/commonTest/resources/example.avsc")
        val text = SystemFileSystem.source(path).buffered().readString()

        val ast = AvroParser.parse(ModuleContent(FileUri("test.ws"), text), true)
        val actual = AvroEmitter.emit(ast.modules.first()).let { json.encodeToString(it) }

        val expected =
            // language=json
            """
            |[
            |    {
            |        "type": "record",
            |        "name": "User",
            |        "fields": [
            |            {
            |                "name": "id",
            |                "type": "int"
            |            },
            |            {
            |                "name": "username",
            |                "type": "string"
            |            },
            |            {
            |                "name": "passwordHash",
            |                "type": "string"
            |            },
            |            {
            |                "name": "signupDate",
            |                "type": "long"
            |            },
            |            {
            |                "name": "emailAddresses",
            |                "type": {
            |                    "type": "array",
            |                    "items": {
            |                        "type": "record",
            |                        "name": "EmailAddress",
            |                        "fields": [
            |                            {
            |                                "name": "address",
            |                                "type": "string"
            |                            },
            |                            {
            |                                "name": "verified",
            |                                "type": "boolean"
            |                            },
            |                            {
            |                                "name": "dateAdded",
            |                                "type": "long"
            |                            },
            |                            {
            |                                "name": "dateBounced",
            |                                "type": [
            |                                    "null",
            |                                    "long"
            |                                ]
            |                            }
            |                        ]
            |                    }
            |                }
            |            },
            |            {
            |                "name": "twitterAccounts",
            |                "type": {
            |                    "type": "array",
            |                    "items": {
            |                        "type": "record",
            |                        "name": "TwitterAccount",
            |                        "fields": [
            |                            {
            |                                "name": "status",
            |                                "type": {
            |                                    "type": "enum",
            |                                    "name": "OAuthStatus",
            |                                    "symbols": [
            |                                        "PENDING",
            |                                        "ACTIVE",
            |                                        "DENIED",
            |                                        "EXPIRED",
            |                                        "REVOKED"
            |                                    ]
            |                                }
            |                            },
            |                            {
            |                                "name": "userId",
            |                                "type": "long"
            |                            },
            |                            {
            |                                "name": "screenName",
            |                                "type": "string"
            |                            },
            |                            {
            |                                "name": "oauthToken",
            |                                "type": "string"
            |                            },
            |                            {
            |                                "name": "oauthTokenSecret",
            |                                "type": [
            |                                    "null",
            |                                    "string"
            |                                ]
            |                            },
            |                            {
            |                                "name": "dateAuthorized",
            |                                "type": "long"
            |                            }
            |                        ]
            |                    }
            |                }
            |            },
            |            {
            |                "name": "toDoItems",
            |                "type": {
            |                    "type": "array",
            |                    "items": {
            |                        "type": "record",
            |                        "name": "ToDoItem",
            |                        "fields": [
            |                            {
            |                                "name": "status",
            |                                "type": {
            |                                    "type": "enum",
            |                                    "name": "ToDoStatus",
            |                                    "symbols": [
            |                                        "HIDDEN",
            |                                        "ACTIONABLE",
            |                                        "DONE",
            |                                        "ARCHIVED",
            |                                        "DELETED"
            |                                    ]
            |                                }
            |                            },
            |                            {
            |                                "name": "title",
            |                                "type": "string"
            |                            },
            |                            {
            |                                "name": "description",
            |                                "type": [
            |                                    "null",
            |                                    "string"
            |                                ]
            |                            },
            |                            {
            |                                "name": "snoozeDate",
            |                                "type": [
            |                                    "null",
            |                                    "long"
            |                                ]
            |                            },
            |                            {
            |                                "name": "subItems",
            |                                "type": {
            |                                    "type": "array",
            |                                    "items": "ToDoItem"
            |                                }
            |                            }
            |                        ]
            |                    }
            |                }
            |            }
            |        ]
            |    },
            |    {
            |        "type": "record",
            |        "name": "EmailAddress",
            |        "fields": [
            |            {
            |                "name": "address",
            |                "type": "string"
            |            },
            |            {
            |                "name": "verified",
            |                "type": "boolean"
            |            },
            |            {
            |                "name": "dateAdded",
            |                "type": "long"
            |            },
            |            {
            |                "name": "dateBounced",
            |                "type": [
            |                    "null",
            |                    "long"
            |                ]
            |            }
            |        ]
            |    },
            |    {
            |        "type": "record",
            |        "name": "TwitterAccount",
            |        "fields": [
            |            {
            |                "name": "status",
            |                "type": {
            |                    "type": "enum",
            |                    "name": "OAuthStatus",
            |                    "symbols": [
            |                        "PENDING",
            |                        "ACTIVE",
            |                        "DENIED",
            |                        "EXPIRED",
            |                        "REVOKED"
            |                    ]
            |                }
            |            },
            |            {
            |                "name": "userId",
            |                "type": "long"
            |            },
            |            {
            |                "name": "screenName",
            |                "type": "string"
            |            },
            |            {
            |                "name": "oauthToken",
            |                "type": "string"
            |            },
            |            {
            |                "name": "oauthTokenSecret",
            |                "type": [
            |                    "null",
            |                    "string"
            |                ]
            |            },
            |            {
            |                "name": "dateAuthorized",
            |                "type": "long"
            |            }
            |        ]
            |    },
            |    {
            |        "type": "enum",
            |        "name": "OAuthStatus",
            |        "symbols": [
            |            "PENDING",
            |            "ACTIVE",
            |            "DENIED",
            |            "EXPIRED",
            |            "REVOKED"
            |        ]
            |    },
            |    {
            |        "type": "record",
            |        "name": "ToDoItem",
            |        "fields": [
            |            {
            |                "name": "status",
            |                "type": {
            |                    "type": "enum",
            |                    "name": "ToDoStatus",
            |                    "symbols": [
            |                        "HIDDEN",
            |                        "ACTIONABLE",
            |                        "DONE",
            |                        "ARCHIVED",
            |                        "DELETED"
            |                    ]
            |                }
            |            },
            |            {
            |                "name": "title",
            |                "type": "string"
            |            },
            |            {
            |                "name": "description",
            |                "type": [
            |                    "null",
            |                    "string"
            |                ]
            |            },
            |            {
            |                "name": "snoozeDate",
            |                "type": [
            |                    "null",
            |                    "long"
            |                ]
            |            },
            |            {
            |                "name": "subItems",
            |                "type": {
            |                    "type": "array",
            |                    "items": "ToDoItem"
            |                }
            |            }
            |        ]
            |    },
            |    {
            |        "type": "enum",
            |        "name": "ToDoStatus",
            |        "symbols": [
            |            "HIDDEN",
            |            "ACTIONABLE",
            |            "DONE",
            |            "ARCHIVED",
            |            "DELETED"
            |        ]
            |    }
            |]
            """.trimMargin()

        actual shouldEqualJson expected
    }

    @Test
    fun compileFullEndpointTest() {
        val result = CompileFullEndpointTest.compiler {
            AvroEmitter
        }
        val expect =
            //language=json
            """
            |[
            |  {
            |    "type": "record",
            |    "name": "PotentialTodoDto",
            |    "fields": [
            |      {
            |        "name": "name",
            |        "type": "string"
            |      },
            |      {
            |        "name": "done",
            |        "type": "boolean"
            |      }
            |    ]
            |  },
            |  {
            |    "type": "record",
            |    "name": "Token",
            |    "fields": [
            |      {
            |        "name": "iss",
            |        "type": "string"
            |      }
            |    ]
            |  },
            |  {
            |    "type": "record",
            |    "name": "TodoDto",
            |    "fields": [
            |      {
            |        "name": "id",
            |        "type": "string"
            |      },
            |      {
            |        "name": "name",
            |        "type": "string"
            |      },
            |      {
            |        "name": "done",
            |        "type": "boolean"
            |      }
            |    ]
            |  },
            |  {
            |    "type": "record",
            |    "name": "Error",
            |    "fields": [
            |      {
            |        "name": "code",
            |        "type": "long"
            |      },
            |      {
            |        "name": "description",
            |        "type": "string"
            |      }
            |    ]
            |  }
            |]
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileMinimalEndpointTest() {
        val result = CompileMinimalEndpointTest.compiler {
            AvroEmitter
        }
        val expect =
            //language=json
            """
            |[
            |  {
            |    "type": "record",
            |    "name": "TodoDto",
            |    "fields": [
            |      {
            |        "name": "description",
            |        "type": "string"
            |      }
            |    ]
            |  }
            |]
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileChannelTest() {
        val result = CompileChannelTest.compiler {
            AvroEmitter
        }
        val expect =
            //language=json
            """
            |[]
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileEnumTest() {
        val result = CompileEnumTest.compiler {
            AvroEmitter
        }
        val expect =
            //language=json
            """
            |[
            |  {
            |    "type": "enum",
            |    "name": "MyAwesomeEnum",
            |    "symbols": [
            |      "ONE",
            |      "Two",
            |      "THREE_MORE",
            |      "UnitedKingdom",
            |      "-1",
            |      "0",
            |      "10",
            |      "-999",
            |      "88"
            |    ]
            |  }
            |]
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileRefinedTest() {
        val result = CompileRefinedTest.compiler {
            AvroEmitter
        }
        val expect =
            //language=json
            """
            |[]
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileUnionTest() {
        val result = CompileUnionTest.compiler {
            AvroEmitter
        }
        val expect =
            //language=json
            """
            |[
            |  {
            |    "name": "UserAccount",
            |    "type": [
            |      "UserAccountPassword",
            |      "UserAccountToken"
            |    ]
            |  },
            |  {
            |    "type": "record",
            |    "name": "UserAccountPassword",
            |    "fields": [
            |      {
            |        "name": "username",
            |        "type": "string"
            |      },
            |      {
            |        "name": "password",
            |        "type": "string"
            |      }
            |    ]
            |  },
            |  {
            |    "type": "record",
            |    "name": "UserAccountToken",
            |    "fields": [
            |      {
            |        "name": "token",
            |        "type": "string"
            |      }
            |    ]
            |  },
            |  {
            |    "type": "record",
            |    "name": "User",
            |    "fields": [
            |      {
            |        "name": "username",
            |        "type": "string"
            |      },
            |      {
            |        "name": "account",
            |        "type": "UserAccount"
            |      }
            |    ]
            |  }
            |]
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }

    @Test
    fun compileTypeTest() {
        val result = CompileTypeTest.compiler { AvroEmitter }
        val expect =
            //language=json
            """
            |[
            |  {
            |    "type": "record",
            |    "name": "Request",
            |    "fields": [
            |      {
            |        "name": "type",
            |        "type": "string"
            |      },
            |      {
            |        "name": "url",
            |        "type": "string"
            |      },
            |      {
            |        "name": "BODY_TYPE",
            |        "type": [
            |          "null",
            |          "string"
            |        ]
            |      },
            |      {
            |        "name": "params",
            |        "type": {
            |          "type": "array",
            |          "items": "string"
            |        }
            |      },
            |      {
            |        "name": "headers",
            |        "type": {
            |          "type": "map",
            |          "values": "string"
            |        }
            |      },
            |      {
            |        "name": "body",
            |        "type": [
            |          "null",
            |          {
            |            "type": "map",
            |            "values": {
            |              "type": "array",
            |              "items": "string"
            |            }
            |          }
            |        ]
            |      }
            |    ]
            |  }
            |]
            """.trimMargin()
        result.shouldBeRight() shouldEqualJson expect
    }
}
