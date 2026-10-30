package community.flock.wirespec.openapi

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import community.flock.wirespec.openapi.common.compile
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test

class StringFormatTest {

    private val schemas =
        // language=json
        """
        |{
        |  "OrderId": { "type": "string", "format": "uuid" },
        |  "Order": {
        |    "type": "object",
        |    "required": ["id", "placedOn"],
        |    "properties": {
        |      "id": { "${'$'}ref": "#/SCHEMAS/OrderId" },
        |      "placedOn": { "type": "string", "format": "date" },
        |      "shippedAt": { "type": "string", "format": "date-time" },
        |      "reference": { "type": "string", "format": "uuid", "pattern": "^[0-9]+${'$'}" },
        |      "email": { "type": "string", "format": "email" },
        |      "website": { "type": "string", "format": "uri" }
        |    }
        |  }
        |}
        """.trimMargin()

    private val v3 =
        // language=json
        """
        |{
        |  "openapi": "3.0.0",
        |  "info": { "title": "Orders", "version": "1.0.0" },
        |  "paths": {},
        |  "components": { "schemas": ${schemas.replace("#/SCHEMAS/", "#/components/schemas/")} }
        |}
        """.trimMargin()

    private val v2 =
        // language=json
        """
        |{
        |  "swagger": "2.0",
        |  "info": { "title": "Orders", "version": "1.0.0" },
        |  "paths": {},
        |  "definitions": ${schemas.replace("#/SCHEMAS/", "#/definitions/")}
        |}
        """.trimMargin()

    private val wirespec =
        // language=ws
        // A component schema of a primitive type is written where it is used.
        """
        |type Order {
        |  id: String(uuid),
        |  placedOn: String(date),
        |  shippedAt: String?(date-time),
        |  reference: String?(/^[0-9]+${'$'}/),
        |  email: String?(email),
        |  website: String?
        |}
        |
        """.trimMargin()

    @Test
    fun testConvertV3Formats() {
        val source = OpenAPIV3Parser.parse(ModuleContent(FileUri("openapi.json"), v3), true)
            .let { WirespecEmitter().emit(it, noLogger).single().result }

        source shouldBe wirespec
        compile(source).shouldBeRight()
    }

    @Test
    fun testEmitV3Formats() {
        OpenAPIV3Emitter
            .emitOpenAPIObject(OpenAPIV3Parser.parse(ModuleContent(FileUri("openapi.json"), v3), true).modules.head.statements, null, noLogger)
            .let { Json.encodeToString(it) }
            .let(Json::parseToJsonElement)
            .properties("components", "schemas", "Order")
            .shouldBeEmitted()
    }

    @Test
    fun testEmitV2Formats() {
        OpenAPIV2Emitter
            .emitSwaggerObject(OpenAPIV2Parser.parse(ModuleContent(FileUri("swagger.json"), v2), true).modules.head.statements, noLogger)
            .let { Json.encodeToString(it) }
            .let(Json::parseToJsonElement)
            .properties("definitions", "Order")
            .shouldBeEmitted()
    }

    private fun JsonElement.properties(vararg path: String): JsonObject = (path.toList() + "properties")
        .fold(this) { element, key -> element.jsonObject.getValue(key) }
        .jsonObject

    // A format goes back as `format`, and a regular expression as a `pattern` without its delimiters.
    private fun JsonObject.shouldBeEmitted() {
        getValue("id").jsonObject["format"] shouldBe JsonPrimitive("uuid")
        getValue("placedOn").jsonObject["format"] shouldBe JsonPrimitive("date")
        getValue("shippedAt").jsonObject.run {
            get("format") shouldBe JsonPrimitive("date-time")
            get("pattern") shouldBe null
        }
        getValue("reference").jsonObject["pattern"] shouldBe JsonPrimitive("^[0-9]+${'$'}")
        getValue("email").jsonObject["format"] shouldBe JsonPrimitive("email")
        getValue("website").jsonObject["format"] shouldBe null
    }

    @Test
    fun testConvertV2Formats() {
        val source = OpenAPIV2Parser.parse(ModuleContent(FileUri("swagger.json"), v2), true)
            .let { WirespecEmitter().emit(it, noLogger).single().result }

        source shouldBe wirespec
        compile(source).shouldBeRight()
    }
}
