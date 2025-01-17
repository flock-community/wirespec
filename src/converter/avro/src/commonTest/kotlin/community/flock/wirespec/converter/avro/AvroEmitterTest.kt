package community.flock.wirespec.converter.avro

import com.goncalossilva.resources.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AvroEmitterTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
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

        assertEquals(expected, actual)
    }


}
