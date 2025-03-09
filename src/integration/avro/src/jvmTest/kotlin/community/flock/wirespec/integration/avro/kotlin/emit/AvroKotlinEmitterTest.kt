package community.flock.wirespec.integration.avro.kotlin.emit

import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.noLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AvroKotlinEmitterTest {

    private val emitter = AvroKotlinEmitter("packageName")

    @Test
    fun emitTypeFunctionBodyTest() {
        val type = Type(
            comment = null,
            identifier = DefinitionIdentifier("Identifier"),
            shape = Type.Shape(
                listOf(
                    Field(
                        identifier = FieldIdentifier(name = "name"),
                        reference = Reference.Primitive(type = Reference.Primitive.Type.String, isNullable = false),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val ast = listOf(type)
        val expected = """
            |package packageName
            |
            |data class Identifier(
            |  val name: String
            |)
            |{
            |  class Avro {
            |    companion object {
            |      val SCHEMA = org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Identifier\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}");
            |
            |      @JvmStatic
            |      fun from(record: org.apache.avro.generic.GenericData.Record): Identifier {
            |        return Identifier(
            |          record.get(0).toString() as String
            |        );
            |      }
            |
            |      @JvmStatic
            |      fun to(model: Identifier ): org.apache.avro.generic.GenericData.Record {
            |        val record = org.apache.avro.generic.GenericData.Record(SCHEMA);
            |        record.put(0, model.name);
            |        return record;
            |      }
            |    }
            |  }
            |
            |}
            |
        """.trimMargin()
        val actual = emitter.emit(ast, noLogger)
        println(actual.first().result)
        assertEquals(expected, actual.first().result)
    }

    @Test
    fun emitEnumFunctionBodyTest() {
        val enum = Enum(
            comment = null,
            identifier = DefinitionIdentifier("Identifier"),
            entries = setOf("ONE", "TWO", "THREE"),
        )
        val ast = listOf(enum)
        val expected = """
            |package packageName
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |enum class Identifier (override val label: String): Wirespec.Enum {
            |  ONE("ONE"),
            |  TWO("TWO"),
            |  THREE("THREE");
            |  override fun toString(): String {
            |    return label
            |  }
            |  class Avro {
            |    companion object {
            |
            |       val SCHEMA: org.apache.avro.Schema = org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"Identifier\",\"symbols\":[\"ONE\",\"TWO\",\"THREE\"]}");
            |
            |       @JvmStatic
            |       fun from(record: org.apache.avro.generic.GenericData.EnumSymbol): Identifier {
            |         return Identifier.valueOf(record.toString());
            |       }
            |
            |       @JvmStatic
            |       fun to(model: Identifier): org.apache.avro.generic.GenericData.EnumSymbol {
            |         return org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, model.name);
            |       }
            |     }
            |  }
            |
            |}
            |
        """.trimMargin()
        val actual = emitter.emit(ast, noLogger)
        println(actual)
        assertEquals(expected, actual.first().result)
    }
}
