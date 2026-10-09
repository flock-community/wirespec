package community.flock.wirespec.converter.avro

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.DefaultValue
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class AvroDefaultValueTest {

    private val json = Json { prettyPrint = true }

    private fun parseWirespec(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source))).getOrNull() ?: error("Parsing failed.")

    private fun AST.defaults(type: String) = modules.flatMap { it.statements }
        .filterIsInstance<Type>()
        .first { it.identifier.value == type }
        .shape.value
        .associate { it.identifier.value to it.defaultValue }

    private val schema =
        // language=json
        """
        |{
        |  "type": "record",
        |  "name": "Settings",
        |  "fields": [
        |    { "name": "name", "type": "string", "default": "anonymous" },
        |    { "name": "retries", "type": "long", "default": 3 },
        |    { "name": "port", "type": "int", "default": 8080 },
        |    { "name": "ratio", "type": "double", "default": 1.5 },
        |    { "name": "weight", "type": "float", "default": 0 },
        |    { "name": "active", "type": "boolean", "default": true },
        |    { "name": "nickname", "type": ["null", "string"], "default": null },
        |    { "name": "title", "type": ["string", "null"], "default": "none" },
        |    { "name": "verified", "type": "boolean", "default": "false" },
        |    { "name": "tags", "type": { "type": "array", "items": "string" }, "default": [] },
        |    { "name": "status", "type": { "type": "enum", "name": "Status", "symbols": ["ACTIVE", "INACTIVE"] }, "default": "ACTIVE" },
        |    { "name": "previous", "type": ["Status", "null"], "default": "INACTIVE" },
        |    { "name": "level", "type": { "type": "enum", "name": "Level", "symbols": ["LOW", "HIGH"] }, "default": "MEDIUM" },
        |    { "name": "comment", "type": ["null", "string"] }
        |  ]
        |}
        """.trimMargin()

    @Test
    fun testParseDefaults() {
        AvroJsonParser.parse(ModuleContent(FileUri("test.avsc"), schema), true).defaults("Settings") shouldBe mapOf(
            "name" to DefaultValue.StringValue("anonymous"),
            "retries" to DefaultValue.IntegerValue("3"),
            "port" to DefaultValue.IntegerValue("8080"),
            "ratio" to DefaultValue.NumberValue("1.5"),
            "weight" to DefaultValue.NumberValue("0.0"),
            "active" to DefaultValue.BooleanValue(true),
            "nickname" to DefaultValue.NullValue,
            "title" to DefaultValue.StringValue("none"),
            // A string on a boolean field, an array and a symbol the enum does not have cannot be Wirespec defaults.
            "verified" to null,
            "tags" to null,
            "status" to DefaultValue.EnumValue("ACTIVE"),
            "previous" to DefaultValue.EnumValue("INACTIVE"),
            "level" to null,
            "comment" to null,
        )
    }

    @Test
    fun testEmitDefaults() {
        val ast = parseWirespec(
            // language=ws
            """
            |type Settings {
            |  name: String = "anonymous",
            |  retries: Integer = 3,
            |  weight: Number32 = 0.5,
            |  active: Boolean = true,
            |  nickname: String? = null,
            |  title: String? = "none",
            |  status: Status? = INACTIVE,
            |  comment: String?
            |}
            |enum Status { ACTIVE, INACTIVE }
            """.trimMargin(),
        )

        AvroJsonEmitter.emit(ast.modules.first()).let { json.encodeToString(it) } shouldEqualJson
            // language=json
            """
            |[
            |  {
            |    "type": "record",
            |    "name": "Settings",
            |    "fields": [
            |      { "name": "name", "type": "string", "default": "anonymous" },
            |      { "name": "retries", "type": "long", "default": 3 },
            |      { "name": "weight", "type": "float", "default": 0.5 },
            |      { "name": "active", "type": "boolean", "default": true },
            |      { "name": "nickname", "type": ["null", "string"], "default": null },
            |      { "name": "title", "type": ["string", "null"], "default": "none" },
            |      { "name": "status", "type": ["Status", "null"], "default": "INACTIVE" },
            |      { "name": "comment", "type": ["null", "string"] }
            |    ]
            |  },
            |  {
            |    "type": "enum",
            |    "name": "Status",
            |    "symbols": ["ACTIVE", "INACTIVE"]
            |  }
            |]
            """.trimMargin()
    }

    @Test
    fun testJsonRoundTrip() {
        val ast = AvroJsonParser.parse(ModuleContent(FileUri("test.avsc"), schema), true)
        val record = AvroJsonEmitter.emit(ast.modules.first()).first().let { json.encodeToString(it) }

        // The emitter writes enums as schemas of their own, which this single record does not include.
        AvroJsonParser.parse(ModuleContent(FileUri("test.avsc"), record), true)
            .defaults("Settings")
            .filterValues { it != null } shouldBe ast.defaults("Settings").filterValues { it != null && it !is DefaultValue.EnumValue }
    }

    @Test
    fun testParseIdlDefaults() {
        val idl =
            """
            |protocol SettingsProtocol {
            |    record Settings {
            |        string name = "anonymous";
            |        long retries = 3;
            |        double ratio = 1.5;
            |        boolean active = true;
            |        union { null, string } nickname = null;
            |        union { string, null } title = "none";
            |        Status status = "INACTIVE";
            |        string comment;
            |    }
            |    enum Status { ACTIVE, INACTIVE }
            |}
            """.trimMargin()

        AvroIdlParser.parse(ModuleContent(FileUri("test.avdl"), idl), true).defaults("Settings") shouldBe mapOf(
            "name" to DefaultValue.StringValue("anonymous"),
            "retries" to DefaultValue.IntegerValue("3"),
            "ratio" to DefaultValue.NumberValue("1.5"),
            "active" to DefaultValue.BooleanValue(true),
            "nickname" to DefaultValue.NullValue,
            "title" to DefaultValue.StringValue("none"),
            "status" to DefaultValue.EnumValue("INACTIVE"),
            "comment" to null,
        )
    }

    @Test
    fun testEmitIdlDefaults() {
        val ast = parseWirespec(
            // language=ws
            """
            |type Settings {
            |  name: String = "say \"hi\"",
            |  retries: Integer = 3,
            |  active: Boolean = true,
            |  nickname: String? = null,
            |  title: String? = "none",
            |  status: Status = INACTIVE
            |}
            |enum Status { ACTIVE, INACTIVE }
            """.trimMargin(),
        )

        AvroIdlEmitter.emit(ast.modules.first()).run {
            shouldContain("string name = \"say \\\"hi\\\"\";")
            shouldContain("long retries = 3;")
            shouldContain("boolean active = true;")
            shouldContain("union { null, string } nickname = null;")
            shouldContain("union { string, null } title = \"none\";")
            shouldContain("Status status = \"INACTIVE\";")
        }
    }
}
