package community.flock.wirespec.converter.asyncapi

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.ParseOptions
import community.flock.wirespec.compiler.core.validate.Validator
import community.flock.wirespec.compiler.test.compile
import community.flock.wirespec.compiler.test.parse
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test

class AsyncAPIParserTest {

    @Test
    fun kitchenSinkV3SurvivesTheRoundTrip() {
        loadResource("kitchen-sink-v3.json").shouldRoundTrip()
    }

    @Test
    fun kitchenSinkV2SurvivesTheRoundTrip() {
        loadResource("kitchen-sink-v2.json").shouldRoundTrip()
    }

    @Test
    fun todoSurvivesTheRoundTrip() {
        loadResource("todo.json").shouldRoundTrip()
    }

    @Test
    fun streetlightsSurvivesTheRoundTrip() {
        loadResource("streetlights.json").shouldRoundTrip()
    }

    @Test
    fun convertsTodoToWirespec() {
        loadResource("todo.json").toWirespec() shouldBe """
            |@AsyncAPIDocument(asyncapi: "3.0.0", info: { title: "Todo events", version: "1.0.0" }, defaultContentType: "application/json")
            |@AsyncAPIComponents
            |@AsyncAPIMessage("TodoUpdated", name: "TodoUpdated", payload: "Todo")
            |@AsyncAPIMessage("TodoCreated", payload: "Todo")
            |@AsyncAPIMessage("TodoDeleted", payload: "TodoDeleted")
            |type AsyncAPI {
            |
            |}
            |
            |@Description("Every change to a todo")
            |@AsyncAPIChannel(address: "todos/{todoId}/updates", parameters: { todoId: { description: "The id of the todo" } })
            |@AsyncAPIMessage("todoUpdated", "$REF": "#/components/messages/TodoUpdated")
            |@AsyncAPISend("publishTodoUpdate", messages: ["todoUpdated"])
            |channel TodoUpdates -> Todo
            |
            |@AsyncAPIChannel(address: "todos.events")
            |@AsyncAPIMessage("todoCreated", "$REF": "#/components/messages/TodoCreated")
            |@AsyncAPIMessage("todoDeleted", "$REF": "#/components/messages/TodoDeleted")
            |@AsyncAPIReceive("onTodoEvent")
            |channel TodoEvents -> TodoEventsMessage
            |
            |@AsyncAPIInline
            |type TodoEventsMessage = Todo | TodoDeleted
            |
            |@AsyncAPIInline
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
            |@AsyncAPIInline
            |type TodoAssignee {
            |  name: String,
            |  @AsyncAPISchema(type: ["string", "\"null\""])
            |  email: String?
            |}
            |
        """.trimMargin()
    }

    @Test
    fun keepsWhatWirespecDoesNotWriteItselfInAsyncAPISchema() {
        """
            {
              "asyncapi": "3.0.0",
              "info": { "title": "Shapes", "version": "1.0.0" },
              "components": {
                "schemas": {
                  "Shape": { "oneOf": [{ "$REF": "#/components/schemas/Circle" }, { "$REF": "#/components/schemas/Square" }] },
                  "Circle": { "type": "object", "required": ["radius"], "properties": { "radius": { "type": "number", "minimum": 0 } } },
                  "Square": { "type": "object", "required": ["side"], "properties": { "side": { "type": "number", "format": "float" } } },
                  "Percentage": { "type": "integer", "minimum": 0, "maximum": 100 },
                  "Node": {
                    "type": "object",
                    "properties": {
                      "parent": { "oneOf": [{ "$REF": "#/components/schemas/Node" }, { "type": "null" }] },
                      "children": { "type": "array", "items": { "$REF": "#/components/schemas/Node" } },
                      "labels": { "type": "object", "additionalProperties": { "type": "string" } },
                      "createdAt": { "type": "string", "format": "date-time" },
                      "email": { "type": "string", "format": "email" },
                      "x-trace.id": { "type": "string" },
                      "external": { "$REF": "other.json#/Thing" },
                      "tags": { "type": "array", "items": { "type": "string" }, "default": [] }
                    }
                  }
                }
              }
            }
        """.toWirespec() shouldBe """
            |@AsyncAPIDocument(asyncapi: "3.0.0", info: { title: "Shapes", version: "1.0.0" })
            |@AsyncAPIComponents
            |type AsyncAPI {
            |
            |}
            |
            |type Shape = Circle | Square
            |
            |type Circle {
            |  @AsyncAPISchema(minimum: "0")
            |  radius: Number
            |}
            |
            |type Square {
            |  side: Number32
            |}
            |
            |type Percentage = Integer(0, 100)
            |
            |type Node {
            |  @AsyncAPISchema(oneOf: "[{\"$REF\":\"#/components/schemas/Node\"},{\"type\":\"null\"}]", omit: ["$REF"])
            |  parent: Node?,
            |  children: Node[]?,
            |  labels: { String }?,
            |  createdAt: String?(date-time),
            |  @AsyncAPISchema(format: "email")
            |  email: String?,
            |  @AsyncAPIName("x-trace.id")
            |  x-trace_id: String?,
            |  @AsyncAPISchema("$REF": "other.json#/Thing")
            |  external: Any?,
            |  @AsyncAPISchema({ default: [ ] })
            |  tags: String[]?
            |}
            |
        """.trimMargin()
    }

    @Test
    fun convertsFormatsToStringFormats() {
        val document = """
            {
              "asyncapi": "3.0.0",
              "info": { "title": "Orders", "version": "1.0.0" },
              "components": {
                "schemas": {
                  "OrderId": { "type": "string", "format": "uuid" },
                  "Order": {
                    "type": "object",
                    "required": ["id"],
                    "properties": {
                      "id": { "$REF": "#/components/schemas/OrderId" },
                      "placedOn": { "type": "string", "format": "date" },
                      "shippedAt": { "type": "string", "format": "date-time" }
                    }
                  }
                }
              }
            }
        """

        document.toWirespec() shouldBe """
            |@AsyncAPIDocument(asyncapi: "3.0.0", info: { title: "Orders", version: "1.0.0" })
            |@AsyncAPIComponents
            |type AsyncAPI {
            |
            |}
            |
            |type OrderId = String(uuid)
            |
            |type Order {
            |  id: OrderId,
            |  placedOn: String?(date),
            |  shippedAt: String?(date-time)
            |}
            |
        """.trimMargin()
        document.shouldRoundTrip()
    }

    @Test
    fun convertedDocumentsCompile() {
        listOf("todo.json", "streetlights.json", "kitchen-sink-v3.json", "kitchen-sink-v2.json").forEach { resource ->
            compile(loadResource(resource).toWirespec())(::KotlinEmitter).shouldBeRight()
        }
    }

    @Test
    fun rejectsWhatIsNoAsyncAPIDocument() {
        shouldThrow<IllegalStateException> { "[]".toWirespec() }.message shouldBe "Invalid AsyncAPI document; expected a JSON object"
        shouldThrow<IllegalStateException> { "{}".toWirespec() }.message shouldBe "Invalid AsyncAPI document; missing 'asyncapi' property"
        shouldThrow<IllegalStateException> { """{ "asyncapi": "1.2.0" }""".toWirespec() }.message shouldBe "Unsupported AsyncAPI version 1.2.0; only 2.x and 3.x can be converted"
    }

    private fun loadResource(name: String): String = SystemFileSystem.source(Path("src/commonTest/resources/$name")).buffered().readString()

    private fun String.toWirespec(): String = AsyncAPIParser.parse(ModuleContent(FileUri("asyncapi.json"), this), strict = true)
        .let { WirespecEmitter().emit(it, noLogger).single().result }

    /**
     * AsyncAPI -> Wirespec AST -> Wirespec source -> Wirespec AST -> AsyncAPI has to give back the document it started
     * from, and the Wirespec parser has to read back the AST the converter produced.
     */
    private fun String.shouldRoundTrip() {
        val converted = AsyncAPIParser.parse(ModuleContent(FileUri("asyncapi.json"), this), strict = true)
            .let { Validator.validate(ParseOptions(), it) }
            .shouldBeRight()
        val parsed = parse(WirespecEmitter().emit(converted, noLogger).single().result).shouldBeRight()

        parsed.modules.head.statements.toList() shouldBe converted.modules.head.statements.toList()
        AsyncAPIEmitter.emit(parsed, noLogger).single().result shouldEqualJson this
    }

    private companion object {
        const val REF = "\$ref"
    }
}
