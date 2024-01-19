package community.flock.wirespec.integration.avro.java.emit

import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.noLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AvroJavaEmitterTest {

    private val emitter = AvroJavaEmitter("packageName", noLogger)

    @Test
    fun emitTypeFunctionBodyTest() {
        val type = Type(
            comment = null,
            identifier = DefinitionIdentifier("identifier"),
            shape = Type.Shape(
                listOf(
                    Field(FieldIdentifier("name"), Reference.Primitive(Reference.Primitive.Type.String), false)
                )
            ),
            extends = emptyList()
        )

        val ast = listOf(type)
        val expected = """
            |package packageName;
            |
            |public record Identifier (
            |  String name
            |) {
            |  public static class Avro {
            |    
            |    public static final org.apache.avro.Schema SCHEMA = 
            |      new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"identifier\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}");
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
        val actual = emitter.emit(ast)
        println(actual.first().result)
        assertEquals(expected, actual.first().result)
    }

    @Test
    fun emitEnumFunctionBodyTest() {
        val enum = Enum(
            comment = null,
            identifier = DefinitionIdentifier("identifier"),
            entries = setOf("ONE", "TWO", "THREE")
        )
        val ast = listOf(enum)
        val expected = """
            |package packageName;
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
            |      new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"identifier\",\"symbols\":[\"ONE\",\"TWO\",\"THREE\"]}");
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
        val actual = emitter.emit(ast)
        println(actual)
        assertEquals(expected, actual.first().result)
    }
}