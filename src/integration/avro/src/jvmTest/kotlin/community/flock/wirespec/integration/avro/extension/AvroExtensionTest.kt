package community.flock.wirespec.integration.avro.extension

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
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
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.ir.extension.ExtendingIrEmitter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AvroExtensionTest {

    private val packageName = PackageName("packageName")

    private val javaEmitter = ExtendingIrEmitter(
        JavaIrEmitter(packageName, EmitShared(true)),
        listOf(AvroExtension(packageName)),
    )

    private val kotlinEmitter = ExtendingIrEmitter(
        KotlinIrEmitter(packageName, EmitShared(true)),
        listOf(AvroExtension(packageName)),
    )

    private fun List<Emitted>.avro(file: String): String? = find { it.file == file }?.result

    private val type = Type(
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

    private val enum = Enum(
        comment = null,
        annotations = emptyList(),
        identifier = DefinitionIdentifier("Identifier"),
        entries = setOf("ONE", "TWO", "THREE"),
    )

    @Test
    fun emitJavaType() {
        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(type))))
        val expected =
            """
            |package packageName.avro;
            |import packageName.model.Identifier;
            |public class IdentifierAvro {
            |  public static final org.apache.avro.Schema SCHEMA =
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Identifier\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}");
            |  public static Identifier from(org.apache.avro.generic.GenericData.Record record) {
            |    return new Identifier(
            |      (String) record.get(0).toString()
            |    );
            |  }
            |  public static org.apache.avro.generic.GenericData.Record to(Identifier data) {
            |    var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
            |      record.put(0, data.name());
            |    return record;
            |  }
            |}
            |
            """.trimMargin()
        assertEquals(expected, javaEmitter.emit(ast, noLogger).avro("packageName/avro/IdentifierAvro.java"))
    }

    @Test
    fun emitJavaEnum() {
        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(enum))))
        val expected =
            """
            |package packageName.avro;
            |import packageName.model.Identifier;
            |public class IdentifierAvro {
            |  public static final org.apache.avro.Schema SCHEMA =
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"Identifier\",\"symbols\":[\"ONE\",\"TWO\",\"THREE\"]}");
            |  public static Identifier from(org.apache.avro.generic.GenericData.EnumSymbol record) {
            |    return Identifier.valueOf(record.toString());
            |  }
            |  public static org.apache.avro.generic.GenericData.EnumSymbol to(Identifier data) {
            |    return new org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, data.name());
            |  }
            |}
            |
            """.trimMargin()
        assertEquals(expected, javaEmitter.emit(ast, noLogger).avro("packageName/avro/IdentifierAvro.java"))
    }

    @Test
    fun emitKotlinType() {
        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(type))))
        val expected =
            """
            |package packageName.avro
            |import packageName.model.Identifier
            |object IdentifierAvro {
            |  val SCHEMA = org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Identifier\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}")
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.Record): Identifier {
            |    return Identifier(
            |      record.get(0).toString() as String
            |    )
            |  }
            |  @JvmStatic
            |  fun to(model: Identifier ): org.apache.avro.generic.GenericData.Record {
            |    val record = org.apache.avro.generic.GenericData.Record(SCHEMA)
            |    record.put(0, model.name)
            |    return record
            |  }
            |}
            |
            """.trimMargin()
        assertEquals(expected, kotlinEmitter.emit(ast, noLogger).avro("packageName/avro/IdentifierAvro.kt"))
    }

    @Test
    fun emitKotlinEnum() {
        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(enum))))
        val expected =
            """
            |package packageName.avro
            |import packageName.model.Identifier
            |object IdentifierAvro {
            |  val SCHEMA: org.apache.avro.Schema = org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"Identifier\",\"symbols\":[\"ONE\",\"TWO\",\"THREE\"]}")
            |  @JvmStatic
            |  fun from(record: org.apache.avro.generic.GenericData.EnumSymbol): Identifier {
            |    return Identifier.valueOf(record.toString())
            |  }
            |  @JvmStatic
            |  fun to(model: Identifier): org.apache.avro.generic.GenericData.EnumSymbol {
            |    return org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, model.name)
            |  }
            |}
            |
            """.trimMargin()
        assertEquals(expected, kotlinEmitter.emit(ast, noLogger).avro("packageName/avro/IdentifierAvro.kt"))
    }
}
