package community.flock.wirespec.converter.asyncapi

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.test.parse
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.json.shouldEqualJson
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

    private fun String.toAsyncAPI(): String = AsyncAPIEmitter.emit(parse(this).shouldBeRight(), noLogger).single().result

    private companion object {
        const val REF = "\$ref"
    }
}
