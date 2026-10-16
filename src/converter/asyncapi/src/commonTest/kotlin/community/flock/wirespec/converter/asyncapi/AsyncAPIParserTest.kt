package community.flock.wirespec.converter.asyncapi

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.test.compile
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test

class AsyncAPIParserTest {

    @Test
    fun convertsAnAsyncAPI3Document() {
        loadResource("todo.json").toWirespec() shouldBe """
            |@Description("Every change to a todo")
            |@AsyncAPIAddress("todos/{todoId}/updates")
            |@AsyncAPISend("publishTodoUpdate")
            |channel TodoUpdates -> Todo
            |
            |@AsyncAPIAddress("todos.events")
            |@AsyncAPIReceive("onTodoEvent")
            |channel TodoEvents -> TodoEventsMessage
            |
            |type TodoEventsMessage = Todo | TodoDeleted
            |
            |type TodoDeleted {
            |  id: TodoId,
            |  reason: String?
            |}
            |
            |type TodoId = String(/^[0-9a-f]{8}$/g)
            |
            |enum Priority {
            |  LOW, MEDIUM, HIGH
            |}
            |
            |@Description("A task to do")
            |type Todo {
            |  id: TodoId,
            |  @Description("What needs doing")
            |  title: String,
            |  done: Boolean,
            |  priority: Priority = MEDIUM,
            |  tags: String[]?,
            |  assignee: TodoAssignee?,
            |  estimate: Integer32? = 1
            |}
            |
            |type TodoAssignee {
            |  name: String,
            |  email: String?
            |}
            |
        """.trimMargin()
    }

    @Test
    fun convertsAnAsyncAPI2Document() {
        loadResource("streetlights.json").toWirespec() shouldBe """
            |@Description("The topic on which measured values may be produced and consumed.")
            |@AsyncAPIAddress("streetlights/{streetlightId}/lighting/measured")
            |@AsyncAPISend("receiveLightMeasurement")
            |channel StreetlightsStreetlightIdLightingMeasured -> LightMeasuredPayload
            |
            |@AsyncAPIAddress("streetlights/{streetlightId}/turn")
            |@AsyncAPIReceive("turnOnOff")
            |channel StreetlightsStreetlightIdTurn -> StreetlightsStreetlightIdTurnMessage
            |
            |type StreetlightsStreetlightIdTurnMessage = TurnOnOffPayload | TurnOffPayload
            |
            |@AsyncAPIAddress("streetlights/{streetlightId}/dim")
            |@AsyncAPIReceive
            |channel StreetlightsStreetlightIdDim -> DimLight
            |
            |type DimLight {
            |  percentage: Integer?,
            |  sentAt: SentAt?
            |}
            |
            |type LightMeasuredPayload {
            |  @Description("Light intensity measured in lumens.")
            |  lumens: Integer?,
            |  sentAt: SentAt?
            |}
            |
            |type TurnOnOffPayload {
            |  @Description("Whether to turn on or off the light.")
            |  command: TurnOnOffPayloadCommand?,
            |  sentAt: SentAt?
            |}
            |
            |enum TurnOnOffPayloadCommand {
            |  `on`, `off`
            |}
            |
            |type TurnOffPayload {
            |  @Description("Whether to turn on or off the light.")
            |  command: TurnOnOffPayloadCommand?,
            |  sentAt: SentAt?,
            |  fadeOut: Boolean?
            |}
            |
            |@Description("Date and time when the message was sent.")
            |type SentAt = String
            |
        """.trimMargin()
    }

    @Test
    fun convertedDocumentsCompile() {
        listOf("todo.json", "streetlights.json").forEach { resource ->
            compile(loadResource(resource).toWirespec())(::KotlinEmitter).shouldBeRight()
        }
    }

    @Test
    fun leavesOutTheAddressAndOperationThatAreWrittenByDefault() {
        """
            {
              "asyncapi": "3.0.0",
              "info": { "title": "Orders", "version": "1.0.0" },
              "channels": {
                "Orders": {
                  "address": "Orders",
                  "messages": {
                    "order": {
                      "payload": { "type": "object", "required": ["id"], "properties": { "id": { "type": "string" } } }
                    }
                  }
                }
              },
              "operations": {
                "sendOrders": { "action": "send", "channel": { "$REF": "#/channels/Orders" } }
              }
            }
        """.toWirespec() shouldBe """
            |channel Orders -> Order
            |
            |type Order {
            |  id: String
            |}
            |
        """.trimMargin()
    }

    @Test
    fun convertsJsonSchema() {
        """
            {
              "asyncapi": "3.0.0",
              "info": { "title": "Shapes", "version": "1.0.0" },
              "components": {
                "schemas": {
                  "Shape": { "oneOf": [{ "$REF": "#/components/schemas/Circle" }, { "$REF": "#/components/schemas/Square" }] },
                  "Circle": { "type": "object", "required": ["radius"], "properties": { "radius": { "type": "number" } } },
                  "Square": { "type": "object", "required": ["side"], "properties": { "side": { "type": "number", "format": "float" } } },
                  "Percentage": { "type": "integer", "minimum": 0, "maximum": 100 },
                  "Ratio": { "type": "number", "minimum": 0, "maximum": 1.5 },
                  "Node": {
                    "type": "object",
                    "properties": {
                      "parent": { "oneOf": [{ "$REF": "#/components/schemas/Node" }, { "type": "null" }] },
                      "children": { "type": "array", "items": { "$REF": "#/components/schemas/Node" } },
                      "labels": { "type": "object", "additionalProperties": { "type": "string" } },
                      "scores": { "type": "array", "items": { "type": ["integer", "null"] } },
                      "data": {},
                      "blob": { "type": "string", "format": "binary" },
                      "shape": { "allOf": [{ "$REF": "#/components/schemas/Shape" }], "description": "The shape of the node" },
                      "kind": { "anyOf": [{ "type": "string" }, { "type": "integer" }] }
                    }
                  }
                }
              }
            }
        """.toWirespec() shouldBe """
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
            |type Percentage = Integer(0, 100)
            |
            |type Ratio = Number(0.0, 1.5)
            |
            |type Node {
            |  parent: Node?,
            |  children: Node[]?,
            |  labels: { String }?,
            |  scores: Integer?[]?,
            |  data: Any?,
            |  blob: Bytes?,
            |  @Description("The shape of the node")
            |  shape: Shape?,
            |  kind: Any?
            |}
            |
        """.trimMargin()
    }

    @Test
    fun convertsAPayloadInAnotherSchemaFormatToAny() {
        """
            {
              "asyncapi": "3.0.0",
              "info": { "title": "Sensors", "version": "1.0.0" },
              "channels": {
                "readings": {
                  "address": "sensors/readings",
                  "messages": {
                    "reading": {
                      "payload": {
                        "schemaFormat": "application/vnd.apache.avro;version=1.9.0",
                        "schema": { "type": "record", "name": "Reading", "fields": [{ "name": "value", "type": "double" }] }
                      }
                    }
                  }
                }
              }
            }
        """.toWirespec() shouldBe """
            |@AsyncAPIAddress("sensors/readings")
            |channel Readings -> Any
            |
        """.trimMargin()
    }

    @Test
    fun rejectsWhatItCannotConvert() {
        shouldThrow<IllegalStateException> { "{}".toWirespec() }.message shouldBe "Invalid AsyncAPI document; missing 'asyncapi' property"
        shouldThrow<IllegalStateException> { """{ "asyncapi": "1.2.0" }""".toWirespec() }.message shouldBe "Unsupported AsyncAPI version 1.2.0; only 2.x and 3.x can be converted"
        shouldThrow<IllegalStateException> {
            """{ "asyncapi": "3.0.0", "components": { "schemas": { "Todo": { "$REF": "todo.json#/Todo" } } } }""".toWirespec()
        }.message shouldBe "Cannot resolve reference todo.json#/Todo; only references within the document are supported"
    }

    private fun loadResource(name: String): String = SystemFileSystem.source(Path("src/commonTest/resources/$name")).buffered().readString()

    private fun String.toWirespec(): String = AsyncAPIParser.parse(ModuleContent(FileUri("asyncapi.json"), this), strict = true)
        .let { WirespecEmitter().emit(it, noLogger).single().result }

    private companion object {
        const val REF = "\$ref"
    }
}
