package community.flock.wirespec.converter.avro

import com.goncalossilva.resources.Resource
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.utils.noLogger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvroEmitterTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun parse(source: String): AST = object : ParseContext {
        override val spec = WirespecSpec
        override val logger = noLogger
    }.parse(source).getOrNull() ?: error("Parsing failed.")

    @Test
    fun testTodoWs() {
        val text = Resource("src/commonTest/resources/todo.ws")
            .apply { assertTrue(exists()) }
            .run { readText() }

        val ast = parse(text)
        val actual = AvroEmitter.emit(ast).let { json.encodeToString(it) }
        val expected = """
            [
                {
                    "type": "enum",
                    "name": "Status",
                    "symbols": [
                        "PUBLIC",
                        "PRIVATE"
                    ]
                },
                {
                    "type": "record",
                    "name": "Left",
                    "fields": [
                        {
                            "name": "left",
                            "type": "string"
                        }
                    ]
                },
                {
                    "type": "record",
                    "name": "Right",
                    "fields": [
                        {
                            "name": "right",
                            "type": "string"
                        }
                    ]
                },
                {
                    "name": "Either",
                    "type": [
                        "Left",
                        "Right"
                    ]
                },
                {
                    "type": "record",
                    "name": "Todo",
                    "fields": [
                        {
                            "name": "id",
                            "type": "string"
                        },
                        {
                            "name": "name",
                            "type": [
                                "null",
                                "string"
                            ]
                        },
                        {
                            "name": "done",
                            "type": "boolean"
                        },
                        {
                            "name": "tags",
                            "type": {
                                "type": "array",
                                "items": "string"
                            }
                        },
                        {
                            "name": "status",
                            "type": "Status"
                        },
                        {
                            "name": "either",
                            "type": "Either"
                        }
                    ]
                }
            ]
        """.trimIndent()

        assertEquals(expected, actual)
    }

    @Test
    fun testSimple() {
        val text = Resource("src/commonTest/resources/example.avsc")
            .apply { assertTrue(exists()) }
            .run { readText() }

        val ast = AvroParser.parse(text)
        val actual = AvroEmitter.emit(ast).let { json.encodeToString(it) }

        val expected = """
        [
            {
                "type": "record",
                "name": "User",
                "fields": [
                    {
                        "name": "id",
                        "type": "int"
                    },
                    {
                        "name": "username",
                        "type": "string"
                    },
                    {
                        "name": "passwordHash",
                        "type": "string"
                    },
                    {
                        "name": "signupDate",
                        "type": "long"
                    },
                    {
                        "name": "emailAddresses",
                        "type": {
                            "type": "array",
                            "items": {
                                "type": "record",
                                "name": "EmailAddress",
                                "fields": [
                                    {
                                        "name": "address",
                                        "type": "string"
                                    },
                                    {
                                        "name": "verified",
                                        "type": "boolean"
                                    },
                                    {
                                        "name": "dateAdded",
                                        "type": "long"
                                    },
                                    {
                                        "name": "dateBounced",
                                        "type": [
                                            "null",
                                            "long"
                                        ]
                                    }
                                ]
                            }
                        }
                    },
                    {
                        "name": "twitterAccounts",
                        "type": {
                            "type": "array",
                            "items": {
                                "type": "record",
                                "name": "TwitterAccount",
                                "fields": [
                                    {
                                        "name": "status",
                                        "type": {
                                            "type": "enum",
                                            "name": "OAuthStatus",
                                            "symbols": [
                                                "PENDING",
                                                "ACTIVE",
                                                "DENIED",
                                                "EXPIRED",
                                                "REVOKED"
                                            ]
                                        }
                                    },
                                    {
                                        "name": "userId",
                                        "type": "long"
                                    },
                                    {
                                        "name": "screenName",
                                        "type": "string"
                                    },
                                    {
                                        "name": "oauthToken",
                                        "type": "string"
                                    },
                                    {
                                        "name": "oauthTokenSecret",
                                        "type": [
                                            "null",
                                            "string"
                                        ]
                                    },
                                    {
                                        "name": "dateAuthorized",
                                        "type": "long"
                                    }
                                ]
                            }
                        }
                    },
                    {
                        "name": "toDoItems",
                        "type": {
                            "type": "array",
                            "items": {
                                "type": "record",
                                "name": "ToDoItem",
                                "fields": [
                                    {
                                        "name": "status",
                                        "type": {
                                            "type": "enum",
                                            "name": "ToDoStatus",
                                            "symbols": [
                                                "HIDDEN",
                                                "ACTIONABLE",
                                                "DONE",
                                                "ARCHIVED",
                                                "DELETED"
                                            ]
                                        }
                                    },
                                    {
                                        "name": "title",
                                        "type": "string"
                                    },
                                    {
                                        "name": "description",
                                        "type": [
                                            "null",
                                            "string"
                                        ]
                                    },
                                    {
                                        "name": "snoozeDate",
                                        "type": [
                                            "null",
                                            "long"
                                        ]
                                    },
                                    {
                                        "name": "subItems",
                                        "type": {
                                            "type": "array",
                                            "items": "ToDoItem"
                                        }
                                    }
                                ]
                            }
                        }
                    }
                ]
            },
            {
                "type": "record",
                "name": "EmailAddress",
                "fields": [
                    {
                        "name": "address",
                        "type": "string"
                    },
                    {
                        "name": "verified",
                        "type": "boolean"
                    },
                    {
                        "name": "dateAdded",
                        "type": "long"
                    },
                    {
                        "name": "dateBounced",
                        "type": [
                            "null",
                            "long"
                        ]
                    }
                ]
            },
            {
                "type": "record",
                "name": "TwitterAccount",
                "fields": [
                    {
                        "name": "status",
                        "type": {
                            "type": "enum",
                            "name": "OAuthStatus",
                            "symbols": [
                                "PENDING",
                                "ACTIVE",
                                "DENIED",
                                "EXPIRED",
                                "REVOKED"
                            ]
                        }
                    },
                    {
                        "name": "userId",
                        "type": "long"
                    },
                    {
                        "name": "screenName",
                        "type": "string"
                    },
                    {
                        "name": "oauthToken",
                        "type": "string"
                    },
                    {
                        "name": "oauthTokenSecret",
                        "type": [
                            "null",
                            "string"
                        ]
                    },
                    {
                        "name": "dateAuthorized",
                        "type": "long"
                    }
                ]
            },
            {
                "type": "enum",
                "name": "OAuthStatus",
                "symbols": [
                    "PENDING",
                    "ACTIVE",
                    "DENIED",
                    "EXPIRED",
                    "REVOKED"
                ]
            },
            {
                "type": "record",
                "name": "ToDoItem",
                "fields": [
                    {
                        "name": "status",
                        "type": {
                            "type": "enum",
                            "name": "ToDoStatus",
                            "symbols": [
                                "HIDDEN",
                                "ACTIONABLE",
                                "DONE",
                                "ARCHIVED",
                                "DELETED"
                            ]
                        }
                    },
                    {
                        "name": "title",
                        "type": "string"
                    },
                    {
                        "name": "description",
                        "type": [
                            "null",
                            "string"
                        ]
                    },
                    {
                        "name": "snoozeDate",
                        "type": [
                            "null",
                            "long"
                        ]
                    },
                    {
                        "name": "subItems",
                        "type": {
                            "type": "array",
                            "items": "ToDoItem"
                        }
                    }
                ]
            },
            {
                "type": "enum",
                "name": "ToDoStatus",
                "symbols": [
                    "HIDDEN",
                    "ACTIONABLE",
                    "DONE",
                    "ARCHIVED",
                    "DELETED"
                ]
            }
        ]
        """.trimIndent()
    }
}
