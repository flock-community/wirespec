package community.flock.wirespec.openapi

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.DefaultValue
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.fields
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import community.flock.wirespec.openapi.common.compile
import community.flock.wirespec.openapi.v2.OpenAPIV2Emitter
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Emitter
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test

class DefaultValueTest {

    private val v3 =
        // language=json
        """
        |{
        |  "openapi": "3.0.0",
        |  "info": { "title": "Settings", "version": "1.0.0" },
        |  "paths": {
        |    "/settings": {
        |      "get": {
        |        "operationId": "GetSettings",
        |        "parameters": [
        |          { "name": "limit", "in": "query", "schema": { "type": "integer", "default": 10 } }
        |        ],
        |        "responses": {
        |          "200": {
        |            "description": "Ok",
        |            "content": { "application/json": { "schema": { "${'$'}ref": "#/components/schemas/Settings" } } }
        |          }
        |        }
        |      }
        |    }
        |  },
        |  "components": {
        |    "schemas": {
        |      "Settings": {
        |        "type": "object",
        |        "required": ["name", "retries", "port", "ratio", "active"],
        |        "properties": {
        |          "name": { "type": "string", "default": "anonymous" },
        |          "retries": { "type": "integer", "format": "int64", "default": 3 },
        |          "port": { "type": "integer", "format": "int32", "default": 8080 },
        |          "ratio": { "type": "number", "default": 2 },
        |          "active": { "type": "boolean", "default": true },
        |          "nickname": { "type": "string", "default": "none" },
        |          "verified": { "type": "boolean", "default": "false" },
        |          "status": { "type": "string", "enum": ["ACTIVE", "INACTIVE"], "default": "ACTIVE" },
        |          "level": { "type": "string", "enum": ["LOW", "HIGH"], "default": "MEDIUM" },
        |          "comment": { "type": "string" }
        |        }
        |      }
        |    }
        |  }
        |}
        """.trimMargin()

    private val v2 =
        // language=json
        """
        |{
        |  "swagger": "2.0",
        |  "info": { "title": "Settings", "version": "1.0.0" },
        |  "paths": {},
        |  "definitions": {
        |    "Settings": {
        |      "type": "object",
        |      "required": ["name", "retries"],
        |      "properties": {
        |        "name": { "type": "string", "default": "anonymous" },
        |        "retries": { "type": "integer", "default": 3 },
        |        "active": { "type": "boolean", "default": true },
        |        "comment": { "type": "string" }
        |      }
        |    }
        |  }
        |}
        """.trimMargin()

    private val wirespec =
        // language=ws
        """
        |type Settings {
        |  name: String = "anonymous",
        |  retries: Integer = 3,
        |  ratio: Number = 1.5,
        |  active: Boolean = true,
        |  nickname: String? = null,
        |  comment: String?
        |}
        """.trimMargin()

    private fun parseWirespec(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source))).getOrNull() ?: error("Parsing failed.")

    private fun AST.defaults(type: String) = modules.flatMap { it.statements }
        .filterIsInstance<Type>()
        .first { it.identifier.value == type }
        .shape.value.fields
        .associate { it.identifier.value to it.defaultValue }

    private fun JsonElement.property(path: String, name: String) = path.split(".")
        .fold(this) { element, key -> element.jsonObject.getValue(key) }
        .jsonObject.getValue(name).jsonObject

    @Test
    fun testParseV3Defaults() {
        OpenAPIV3Parser.parse(ModuleContent(FileUri("openapi.json"), v3), true).defaults("Settings") shouldBe mapOf(
            "name" to DefaultValue.StringValue("anonymous"),
            "retries" to DefaultValue.IntegerValue("3"),
            "port" to DefaultValue.IntegerValue("8080"),
            "ratio" to DefaultValue.NumberValue("2.0"),
            "active" to DefaultValue.BooleanValue(true),
            "nickname" to DefaultValue.StringValue("none"),
            // A string on a boolean property and a value the enum does not have cannot be Wirespec defaults.
            "verified" to null,
            "status" to DefaultValue.EnumValue("ACTIVE"),
            "level" to null,
            "comment" to null,
        )
    }

    @Test
    fun testConvertV3ToWirespec() {
        val ast = OpenAPIV3Parser.parse(ModuleContent(FileUri("openapi.json"), v3), true)
        val source = WirespecEmitter().emit(ast, noLogger).joinToString("\n") { it.result }

        source.run {
            shouldContain("name: String = \"anonymous\"")
            shouldContain("retries: Integer = 3")
            shouldContain("port: Integer32 = 8080")
            shouldContain("ratio: Number = 2.0")
            shouldContain("active: Boolean = true")
            shouldContain("nickname: String? = \"none\"")
            shouldContain("status: SettingsStatus? = ACTIVE")
            // Defaults on query parameters are not part of the Wirespec language.
            shouldContain("?{limit: Integer?}")
        }
        compile(source).shouldBeRight()
    }

    @Test
    fun testEmitV3Defaults() {
        val openapi = OpenAPIV3Emitter
            .emitOpenAPIObject(parseWirespec(wirespec).modules.head.statements, null, noLogger)
            .let { Json.encodeToString(it) }
            .let(Json::parseToJsonElement)

        openapi.run {
            property("components.schemas.Settings.properties", "name")["default"] shouldBe JsonPrimitive("anonymous")
            property("components.schemas.Settings.properties", "retries")["default"] shouldBe JsonPrimitive(3)
            property("components.schemas.Settings.properties", "ratio")["default"] shouldBe JsonPrimitive(1.5)
            property("components.schemas.Settings.properties", "active")["default"] shouldBe JsonPrimitive(true)
            property("components.schemas.Settings.properties", "nickname")["default"] shouldBe null
            property("components.schemas.Settings.properties", "comment")["default"] shouldBe null
        }
    }

    @Test
    fun testParseV2Defaults() {
        OpenAPIV2Parser.parse(ModuleContent(FileUri("swagger.json"), v2), true).defaults("Settings") shouldBe mapOf(
            "name" to DefaultValue.StringValue("anonymous"),
            "retries" to DefaultValue.IntegerValue("3"),
            "active" to DefaultValue.BooleanValue(true),
            "comment" to null,
        )
    }

    @Test
    fun testEmitV2Defaults() {
        val swagger = OpenAPIV2Emitter
            .emitSwaggerObject(parseWirespec(wirespec).modules.head.statements, noLogger)
            .let { Json.encodeToString(it) }
            .let(Json::parseToJsonElement)

        swagger.run {
            property("definitions.Settings.properties", "name")["default"] shouldBe JsonPrimitive("anonymous")
            property("definitions.Settings.properties", "retries")["default"] shouldBe JsonPrimitive(3)
            property("definitions.Settings.properties", "active")["default"] shouldBe JsonPrimitive(true)
            property("definitions.Settings.properties", "nickname")["default"] shouldBe null
        }
    }
}
