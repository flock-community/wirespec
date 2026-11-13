package community.flock.wirespec.converter.asyncapi

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.emit.SpecInfo
import community.flock.wirespec.compiler.test.compile
import community.flock.wirespec.compiler.test.parse
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test

class AsyncAPIEmitterTest {

    private val wirespec = """
        |@Description("Every change to a todo")
        |@AsyncAPIChannel(address: "todos/{todoId}/updates")
        |@AsyncAPIReceive("onTodoUpdate")
        |channel TodoUpdates -> Todo
        |
        |channel TodoIds -> TodoId[]
        |
        |endpoint GetTodos GET /todos -> {
        |  200 -> Todo[]
        |}
        |
        |rpc CompleteTodo {
        |  id: TodoId
        |} -> Todo
        |
        |type TodoId = String(/^[0-9a-f\/]+$/g)
        |
        |enum Prio {
        |  LOW, MEDIUM, HIGH
        |}
        |
        |// A task to do
        |type Todo {
        |  id: TodoId,
        |  @Description("What needs doing")
        |  title: String,
        |  prio: Prio = MEDIUM,
        |  tags: String?[]?,
        |  estimate: Integer32 = 1
        |}
    """.trimMargin()

    @Test
    fun emitsWirespecThatDoesNotComeFromAsyncAPI() {
        wirespec.toAsyncAPI() shouldEqualJson """
            {
              "asyncapi": "3.0.0",
              "info": { "title": "Wirespec", "version": "0.0.0" },
              "defaultContentType": "application/json",
              "channels": {
                "todoUpdates": {
                  "address": "todos/{todoId}/updates",
                  "description": "Every change to a todo",
                  "messages": { "Todo": { "$REF": "#/components/messages/Todo" } }
                },
                "todoIds": {
                  "address": "todoIds",
                  "messages": { "TodoIdsMessage": { "$REF": "#/components/messages/TodoIdsMessage" } }
                }
              },
              "operations": {
                "onTodoUpdate": {
                  "action": "receive",
                  "channel": { "$REF": "#/channels/todoUpdates" },
                  "messages": [{ "$REF": "#/channels/todoUpdates/messages/Todo" }]
                },
                "sendTodoIds": {
                  "action": "send",
                  "channel": { "$REF": "#/channels/todoIds" },
                  "messages": [{ "$REF": "#/channels/todoIds/messages/TodoIdsMessage" }]
                }
              },
              "components": {
                "schemas": {
                  "TodoId": { "type": "string", "pattern": "^[0-9a-f/]+$" },
                  "Prio": { "type": "string", "enum": ["LOW", "MEDIUM", "HIGH"] },
                  "Todo": {
                    "type": "object",
                    "description": "A task to do",
                    "properties": {
                      "id": { "$REF": "#/components/schemas/TodoId" },
                      "title": { "type": "string", "description": "What needs doing" },
                      "prio": { "$REF": "#/components/schemas/Prio", "default": "MEDIUM" },
                      "tags": { "type": "array", "items": { "type": ["string", "null"] } },
                      "estimate": { "type": "integer", "format": "int32", "default": 1 }
                    },
                    "required": ["id", "title", "prio", "estimate"]
                  }
                },
                "messages": {
                  "Todo": { "name": "Todo", "payload": { "$REF": "#/components/schemas/Todo" } },
                  "TodoIdsMessage": { "name": "TodoIdsMessage", "payload": { "type": "array", "items": { "$REF": "#/components/schemas/TodoId" } } }
                }
              }
            }
        """
    }

    @Test
    fun asyncAPIWrittenFromWirespecConvertsBackWithoutLoss() {
        val asyncAPI = wirespec.toAsyncAPI()

        AsyncAPIParser.parse(ModuleContent(FileUri("asyncapi.json"), asyncAPI), strict = true)
            .let { AsyncAPIEmitter.emit(it, noLogger).single().result } shouldEqualJson asyncAPI
    }

    @Test
    fun readsTheSpecAnnotationsOfEveryFileInFileOrder() {
        val events = """
            |@spec:AsyncAPIDocument(asyncapi: "3.0.0", info: { title: "Todo events", version: "1.0.0" })
            |
            |@AsyncAPIChannel(address: "todos")
            |@AsyncAPIMessage("todo", "$REF": "#/components/messages/Todo")
            |channel Todos -> Todo
            |
            |type Todo {
            |  id: String
            |}
        """.trimMargin()
        val messages = """
            |@spec:AsyncAPIMessage("Todo", payload: "Todo")
            |@spec:AsyncAPIMessage("Ping", payload: "String")
        """.trimMargin()

        AsyncAPIEmitter.emit(parse(events, messages).shouldBeRight(), noLogger).single().result shouldEqualJson """
            {
              "asyncapi": "3.0.0",
              "info": { "title": "Todo events", "version": "1.0.0" },
              "channels": {
                "todos": { "address": "todos", "messages": { "todo": { "$REF": "#/components/messages/Todo" } } }
              },
              "components": {
                "schemas": {
                  "Todo": { "type": "object", "properties": { "id": { "type": "string" } }, "required": ["id"] }
                },
                "messages": {
                  "Todo": { "payload": { "$REF": "#/components/schemas/Todo" } },
                  "Ping": { "payload": { "type": "string" } }
                }
              }
            }
        """
    }

    @Test
    fun addsTheMessagesAndOperationsOfTheFileHeaderToTheDefaultDocument() {
        """
            |@spec:AsyncAPIMessage("Ping", payload: "String")
            |@spec:AsyncAPISend("pingAll", channel: { "$REF": "#/channels/todoIds" })
            |
            |channel TodoIds -> String
        """.trimMargin().toAsyncAPI() shouldEqualJson """
            {
              "asyncapi": "3.0.0",
              "info": { "title": "Wirespec", "version": "0.0.0" },
              "defaultContentType": "application/json",
              "channels": {
                "todoIds": {
                  "address": "todoIds",
                  "messages": { "TodoIdsMessage": { "$REF": "#/components/messages/TodoIdsMessage" } }
                }
              },
              "operations": {
                "sendTodoIds": {
                  "action": "send",
                  "channel": { "$REF": "#/channels/todoIds" },
                  "messages": [{ "$REF": "#/channels/todoIds/messages/TodoIdsMessage" }]
                },
                "pingAll": { "action": "send", "channel": { "$REF": "#/channels/todoIds" } }
              },
              "components": {
                "messages": {
                  "Ping": { "payload": { "type": "string" } },
                  "TodoIdsMessage": { "name": "TodoIdsMessage", "payload": { "type": "string" } }
                }
              }
            }
        """
    }

    @Test
    fun rejectsWhatTheDocumentHasOnceDeclaredInTwoFiles() {
        val todos = """
            |@spec:AsyncAPIDocument(asyncapi: "3.0.0", info: { title: "Todos", version: "1.0.0" })
            |@spec:AsyncAPIMessage("Todo", payload: "Todo")
            |@spec:AsyncAPISend("publishAll", channel: { "$REF": "#/channels/todos" })
            |
            |type Todo {
            |  id: String
            |}
        """.trimMargin()
        val audit = """
            |@spec:AsyncAPIDocument(asyncapi: "2.6.0", info: { title: "Audit", version: "1.0.0" })
            |@spec:AsyncAPIMessage("Todo", payload: "String")
            |@spec:AsyncAPIReceive("publishAll")
            |@spec:AsyncAPIMessage("Audit", payload: "String")
        """.trimMargin()

        AsyncAPIEmitter.validate(parse(todos, audit).shouldBeRight()).map { it.fileUri.value to it.message } shouldBe listOf(
            "test1.ws" to "@spec:AsyncAPIDocument is declared 2 times (in test0.ws, test1.ws), but the AsyncAPI document can only have one",
            "test1.ws" to "The component message \"Todo\" is declared 2 times (in test0.ws, test1.ws), but the AsyncAPI document can only have one",
            "test1.ws" to "The operation \"publishAll\" is declared 2 times (in test0.ws, test1.ws), but the AsyncAPI document can only have one",
        )
    }

    @Test
    fun rejectsDocumentAnnotationsOnADefinition() {
        val holder = """
            |@AsyncAPIDocument(asyncapi: "3.0.0", info: { title: "Todos", version: "1.0.0" })
            |@AsyncAPIComponents
            |type AsyncAPI {}
        """.trimMargin()

        AsyncAPIEmitter.validate(parse(holder).shouldBeRight()).map { it.message } shouldBe listOf(
            "@AsyncAPIDocument on AsyncAPI describes the whole AsyncAPI document, so it belongs in the file header: write it as @spec:AsyncAPIDocument",
            "@AsyncAPIComponents on AsyncAPI describes the whole AsyncAPI document, so it belongs in the file header: write it as @spec:AsyncAPIComponents",
        )
        compile(holder).invoke { AsyncAPIEmitter }.shouldBeLeft().map { it.message }.shouldHaveSize(2)
    }

    @Test
    fun acceptsWirespecThatDoesNotComeFromAsyncAPI() {
        AsyncAPIEmitter.validate(parse(wirespec).shouldBeRight()) shouldBe emptyList()
    }

    @Test
    fun theBuildSetsTheTitleAndVersionOfTheDocument() {
        val converted =
            // language=ws
            """
            |@spec:AsyncAPIDocument(asyncapi: "3.0.0", info: { title: "Todo events", version: "1.0.0", description: "Everything that happens to a todo" })
            |
            |type Todo { id: String }
            """.trimMargin()

        listOf(
            converted to """{ "title": "Todo events", "version": "1.1.0-SNAPSHOT", "description": "Everything that happens to a todo" }""",
            wirespec to """{ "title": "Wirespec", "version": "1.1.0-SNAPSHOT" }""",
        ).forEach { (source, info) ->
            AsyncAPIEmitter.withSpecInfo(SpecInfo(version = "1.1.0-SNAPSHOT"))
                .emit(parse(source).shouldBeRight(), noLogger)
                .single()
                .result
                .let { Json.parseToJsonElement(it).jsonObject.getValue("info").toString() } shouldEqualJson info
        }
    }

    @Test
    fun writesTheInfoServersSecuritySchemesAndTagsOfTheSpec() {
        val source =
            // language=ws
            """
            |@spec:Description("Everything that happens to a todo")
            |@spec:AsyncAPIInfo(title: "Todo events", version: "1.0.0", contact: { name: "Team", email: "team@example.com" })
            |@spec:AsyncAPIServer("production", host: "broker.example.com:9092", protocol: "kafka", description: "Production", security: ["saslScram"])
            |@spec:AsyncAPISecurityScheme("saslScram", type: "scramSha256", description: "SASL/SCRAM")
            |@spec:AsyncAPITag("todos", description: "Todo events")
            |
            |type Todo { id: String }
            |
            |channel TodoUpdates -> Todo
            """.trimMargin()

        val document = Json.parseToJsonElement(source.toAsyncAPI()).jsonObject

        document.getValue("info").toString() shouldEqualJson """
            {
              "title": "Todo events",
              "version": "1.0.0",
              "contact": { "name": "Team", "email": "team@example.com" },
              "description": "Everything that happens to a todo",
              "tags": [{ "name": "todos", "description": "Todo events" }]
            }
        """
        document.getValue("servers").toString() shouldEqualJson """
            {
              "production": {
                "host": "broker.example.com:9092",
                "protocol": "kafka",
                "description": "Production",
                "security": [{ "$REF": "#/components/securitySchemes/saslScram" }]
              }
            }
        """
        document.getValue("components").jsonObject.getValue("securitySchemes").toString() shouldEqualJson """
            { "saslScram": { "type": "scramSha256", "description": "SASL/SCRAM" } }
        """
        AsyncAPIEmitter.withSpecInfo(SpecInfo(version = "1.1.0")).emit(parse(source).shouldBeRight(), noLogger).single().result
            .let { Json.parseToJsonElement(it).jsonObject.getValue("info").jsonObject.getValue("version").toString() } shouldBe "\"1.1.0\""
    }

    @Test
    fun writesTheSpecAnnotationsInTheShapeOfAsyncAPI2() {
        val source =
            // language=ws
            """
            |@spec:AsyncAPIDocument(asyncapi: "2.6.0")
            |@spec:AsyncAPIInfo(title: "Todo events", version: "1.0.0")
            |@spec:AsyncAPIServer("production", url: "broker.example.com:9092", protocol: "kafka", security: ["saslScram"])
            |@spec:AsyncAPISecurityScheme("saslScram", type: "scramSha256")
            |@spec:AsyncAPITag("todos")
            |
            |type Todo { id: String }
            """.trimMargin()

        val document = Json.parseToJsonElement(source.toAsyncAPI()).jsonObject

        document.getValue("info").toString() shouldEqualJson """{ "title": "Todo events", "version": "1.0.0" }"""
        document.getValue("tags").toString() shouldEqualJson """[{ "name": "todos" }]"""
        document.getValue("servers").toString() shouldEqualJson """
            { "production": { "url": "broker.example.com:9092", "protocol": "kafka", "security": [{ "saslScram": [] }] } }
        """
    }

    @Test
    fun rejectsSpecAnnotationsThatConflictOrPointNowhere() {
        val first =
            // language=ws
            """
            |@spec:AsyncAPIDocument(asyncapi: "3.0.0", info: { title: "Todo events", version: "1.0.0" }, servers: { production: { host: "a", protocol: "kafka" } })
            |@spec:AsyncAPIComponents(securitySchemes: { saslScram: { type: "scramSha256" } })
            |@spec:AsyncAPIInfo(title: "Todo events", version: "1.0.0")
            |@spec:AsyncAPIServer("production", host: "b", protocol: "kafka", security: ["oauth"])
            |@spec:AsyncAPISecurityScheme("saslScram", type: "scramSha512")
            |@spec:AsyncAPITag(description: "No name")
            |@file:AsyncAPIServer("local", host: "localhost", protocol: "kafka")
            |
            |@AsyncAPIServer("staging", host: "c", protocol: "kafka")
            |channel TodoUpdates -> String
            """.trimMargin()
        val second =
            // language=ws
            """
            |@spec:AsyncAPIInfo(title: "Other", version: "2.0.0")
            |
            |type Todo { id: String }
            """.trimMargin()

        AsyncAPIEmitter.validate(parse(first, second).shouldBeRight()).map { it.message } shouldBe listOf(
            "@AsyncAPIServer on TodoUpdates describes the whole AsyncAPI document, so it belongs in the file header: write it as @spec:AsyncAPIServer",
            "@file:AsyncAPIServer in test0.ws: an AsyncAPI document has no metadata per file, so write @spec:AsyncAPIServer",
            "@spec:AsyncAPIInfo is declared 2 times (in test0.ws, test1.ws), but the AsyncAPI document can only have one",
            "@spec:AsyncAPITag in test0.ws has no name",
            "The info of the document is declared in @spec:AsyncAPIInfo and in @spec:AsyncAPIDocument (test0.ws)",
            "The server \"production\" is declared in @spec:AsyncAPIServer and in the servers of @spec:AsyncAPIDocument (test0.ws)",
            "The security scheme \"saslScram\" is declared in @spec:AsyncAPISecurityScheme and in the securitySchemes of @spec:AsyncAPIComponents (test0.ws)",
            "The info of the document is declared in @spec:AsyncAPIInfo and in @spec:AsyncAPIDocument (test1.ws)",
            "@spec:AsyncAPIServer(\"production\") in test0.ws names the security scheme oauth, which no @spec:AsyncAPISecurityScheme declares",
        )
    }

    private fun String.toAsyncAPI(): String = AsyncAPIEmitter.emit(parse(this).shouldBeRight(), noLogger).single().result

    private companion object {
        const val REF = "\$ref"
    }
}
