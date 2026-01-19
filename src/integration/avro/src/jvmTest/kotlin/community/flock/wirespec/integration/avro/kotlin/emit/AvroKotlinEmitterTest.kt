package community.flock.wirespec.integration.avro.kotlin.emit

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.utils.noLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AvroKotlinEmitterTest {

    private val emitter = AvroEmitter(PackageName("packageName"), EmitShared(true))

    @Test
    fun emitTypeFunctionBodyTest() {
        val type = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Identifier"),
            shape = Type.Shape(
                listOf(
                    Field(
                        identifier = FieldIdentifier(name = "name"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(type))))
        val expected = """
            |package packageName.avro
            |
            |import packageName.model.Identifier
            |
            |object IdentifierAvro {
            |  val SCHEMA = org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Identifier\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}")
            |
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.Record): Identifier {
            |    return Identifier(
            |      record.get(0).toString() as String
            |    )
            |  }
            |
            |  @JvmStatic
            |  fun to(model: Identifier ): org.apache.avro.generic.GenericData.Record {
            |    val record = org.apache.avro.generic.GenericData.Record(SCHEMA)
            |    record.put(0, model.name)
            |    return record
            |  }
            |
            |}
            |
        """.trimMargin()
        val actual = emitter.emit(ast, noLogger)
        println(actual.first().result)
        assertEquals(expected, actual.find { it.file == "packageName/avro/IdentifierAvro.kt" }?.result)
    }

    @Test
    fun emitEnumFunctionBodyTest() {
        val enum = Enum(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Identifier"),
            entries = setOf("ONE", "TWO", "THREE"),
        )
        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(enum))))
        val expected = """
            |package packageName.avro
            |
            |import packageName.model.Identifier
            |
            |object IdentifierAvro {
            |
            |  val SCHEMA: org.apache.avro.Schema = org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"Identifier\",\"symbols\":[\"ONE\",\"TWO\",\"THREE\"]}")
            |
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.EnumSymbol): Identifier {
            |    return Identifier.valueOf(record.toString())
            |  }
            |
            |  @JvmStatic
            |  fun to(model: Identifier): org.apache.avro.generic.GenericData.EnumSymbol {
            |    return org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, model.name)
            |  }
            |
            |}
            |
        """.trimMargin()
        val actual = emitter.emit(ast, noLogger)
        println(actual)
        assertEquals(expected, actual.find { it.file == "packageName/avro/IdentifierAvro.kt" }?.result)
    }
}
