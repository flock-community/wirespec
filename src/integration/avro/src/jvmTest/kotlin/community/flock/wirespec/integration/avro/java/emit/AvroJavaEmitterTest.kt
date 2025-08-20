package community.flock.wirespec.integration.avro.java.emit

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.noLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AvroJavaEmitterTest {

    private val emitter = AvroEmitter(PackageName("packageName"), EmitShared(true))

    @Test
    fun emitRootFunctionBodyTest() {
        val type = Type(
            comment = null,
            identifier = DefinitionIdentifier("Identifier"),
            shape = Type.Shape(
                listOf(
                    Field(
                        identifier = FieldIdentifier(name = "name"),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.String(null), isNullable = false),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(type))))
        val expected = """
            |package packageName.model;
            |
            |public record Identifier (
            |  String name
            |) {
            |  public static class Avro {
            |    
            |    public static final org.apache.avro.Schema SCHEMA = 
            |      new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Identifier\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}");
            |
            |    public static Identifier from(org.apache.avro.generic.GenericData.Record record) {
            |       return new Identifier(
            |       (String) record.get(0).toString()
            |      );
            |    }
            |    
            |    public static org.apache.avro.generic.GenericData.Record to(Identifier data) {
            |      var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
            |      record.put(0, data.name());
            |      return record;
            |    }
            |  }
            |};
            |
        """.trimMargin()
        val actual = emitter.emit(ast, noLogger)
        assertEquals(expected, actual.first().result)
    }

    @Test
    fun emitEnumFunctionBodyTest() {
        val enum = Enum(
            comment = null,
            identifier = DefinitionIdentifier("Identifier"),
            entries = setOf("ONE", "TWO", "THREE"),
        )
        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(enum))))
        val expected = """
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public enum Identifier implements Wirespec.Enum {
            |  ONE("ONE"),
            |  TWO("TWO"),
            |  THREE("THREE");
            |  public final String label;
            |  Identifier(String label) {
            |    this.label = label;
            |  }
            |  @Override
            |  public String toString() {
            |    return label;
            |  }
            |  @Override
            |  public String getLabel() {
            |    return label;
            |  }
            |  public static class Avro {
            |
            |    public static final org.apache.avro.Schema SCHEMA = 
            |      new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"Identifier\",\"symbols\":[\"ONE\",\"TWO\",\"THREE\"]}");
            |    
            |    public static Identifier from(org.apache.avro.generic.GenericData.EnumSymbol record) {
            |      return Identifier.valueOf(record.toString());
            |    }
            |    
            |    public static org.apache.avro.generic.GenericData.EnumSymbol to(Identifier data) {
            |      return new org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, data.name());
            |    }
            |  }
            |}
            |
        """.trimMargin()
        val actual = emitter.emit(ast, noLogger)
        assertEquals(expected, actual.first().result)
    }
}
