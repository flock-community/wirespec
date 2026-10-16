package community.flock.wirespec.converter.asyncapi

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.test.parse
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test

class AsyncAPIEmitterTest {

    @Test
    fun emitsChannelsOperationsAndSchemas() {
        """
            |@Description("Every change to a todo")
            |@AsyncAPIAddress("todos/{todoId}/updates")
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
        """.trimMargin().toAsyncAPI() shouldEqualJson """
            {
              "asyncapi": "3.0.0",
              "info": { "title": "Wirespec", "version": "0.0.0" },
              "defaultContentType": "application/json",
              "channels": {
                "TodoUpdates": {
                  "address": "todos/{todoId}/updates",
                  "description": "Every change to a todo",
                  "messages": { "Todo": { "$REF": "#/components/messages/Todo" } }
                },
                "TodoIds": {
                  "address": "TodoIds",
                  "messages": { "TodoIdsMessage": { "$REF": "#/components/messages/TodoIdsMessage" } }
                }
              },
              "operations": {
                "onTodoUpdate": {
                  "action": "receive",
                  "channel": { "$REF": "#/channels/TodoUpdates" },
                  "messages": [{ "$REF": "#/channels/TodoUpdates/messages/Todo" }]
                },
                "sendTodoIds": {
                  "action": "send",
                  "channel": { "$REF": "#/channels/TodoIds" },
                  "messages": [{ "$REF": "#/channels/TodoIds/messages/TodoIdsMessage" }]
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
                      "prio": { "allOf": [{ "$REF": "#/components/schemas/Prio" }], "default": "MEDIUM" },
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
    fun wirespecSurvivesTheRoundTrip() {
        val wirespec = """
            |@AsyncAPIAddress("todos.updates")
            |@AsyncAPISend("publishTodo")
            |@AsyncAPIReceive
            |channel TodoUpdates -> Todo
            |
            |channel Shapes -> Shape
            |
            |type TodoId = String(/^[0-9a-f]+$/g)
            |
            |type Percentage = Integer32(0, 100)
            |
            |type Ratio = Number(0.0, 1.5)
            |
            |enum Prio {
            |  LOW, MEDIUM, HIGH
            |}
            |
            |@Description("A task to do")
            |type Todo {
            |  id: TodoId,
            |  @Description("What needs doing")
            |  title: String,
            |  prio: Prio = MEDIUM,
            |  done: Boolean = false,
            |  progress: Percentage?,
            |  tags: String?[]?,
            |  attributes: { Any },
            |  attachment: Bytes?,
            |  related: Todo[]?
            |}
            |
            |type Shape = Circle | Square
            |
            |type Circle {
            |  radius: Number
            |}
            |
            |type Square {
            |  side: Number32
            |}
            |
        """.trimMargin()

        wirespec.toAsyncAPI().toAST().toWirespec() shouldBe wirespec
    }

    @Test
    fun asyncAPISurvivesTheRoundTrip() {
        val wirespec = loadResource("todo.json").toAST().toWirespec()

        wirespec.toAsyncAPI().toAST().toWirespec() shouldBe wirespec
    }

    private fun loadResource(name: String): String = SystemFileSystem.source(Path("src/commonTest/resources/$name")).buffered().readString()

    private fun String.toAsyncAPI(): String = AsyncAPIEmitter.emit(parse(this).shouldBeRight(), noLogger).single().result

    private fun String.toAST(): AST = AsyncAPIParser.parse(ModuleContent(FileUri("asyncapi.json"), this), strict = true)

    private fun AST.toWirespec(): String = WirespecEmitter().emit(this, noLogger).single().result

    private companion object {
        const val REF = "\$ref"
    }
}
