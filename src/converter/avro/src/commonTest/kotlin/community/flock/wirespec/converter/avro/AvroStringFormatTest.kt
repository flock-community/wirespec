package community.flock.wirespec.converter.avro

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.StringFormat
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.fields
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AvroStringFormatTest {

    private val uuid = Reference.Primitive.Type.String(StringFormat.UUID.regExp)

    private fun AST.references(type: String) = modules.flatMap { it.statements }
        .filterIsInstance<Type>()
        .first { it.identifier.value == type }
        .shape.fields
        .associate { it.identifier.value to it.reference }

    @Test
    fun testParseJsonUuid() {
        val schema =
            // language=json
            """
            |{
            |  "type": "record",
            |  "name": "Order",
            |  "fields": [
            |    { "name": "id", "type": { "type": "string", "logicalType": "uuid" } },
            |    { "name": "reference", "type": ["null", { "type": "string", "logicalType": "uuid" }] },
            |    { "name": "placedOn", "type": { "type": "int", "logicalType": "date" } }
            |  ]
            |}
            """.trimMargin()

        AvroJsonParser.parse(ModuleContent(FileUri("test.avsc"), schema), true).references("Order") shouldBe mapOf(
            "id" to Reference.Primitive(uuid, isNullable = false),
            "reference" to Reference.Primitive(uuid, isNullable = true),
            // An Avro date counts days, so it is no string.
            "placedOn" to Reference.Primitive(Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32, null), isNullable = false),
        )
    }

    @Test
    fun testParseIdlUuid() {
        val source =
            """
            |protocol Orders {
            |    record Order {
            |        uuid id;
            |        uuid? reference;
            |    }
            |}
            """.trimMargin()

        AvroIdlParser.parse(ModuleContent(FileUri("test.avdl"), source), true).references("Order") shouldBe mapOf(
            "id" to Reference.Primitive(uuid, isNullable = false),
            "reference" to Reference.Primitive(uuid, isNullable = true),
        )
    }
}
