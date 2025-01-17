package community.flock.wirespec.integration.avro.java.emit

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.integration.avro.Utils
import community.flock.wirespec.integration.avro.Utils.isEnum

class AvroJavaEmitter(private val packageName: String, logger: Logger) : JavaEmitter(packageName, logger) {

    private fun emitAvroSchema(type: Definition, ast: AST) = Utils.emitAvroSchema(packageName, type, ast)
        ?.replace("\\\"<<<<<", "\" + ")
        ?.replace(">>>>>\\\"", ".Avro.SCHEMA + \"")
        ?: error("Cannot emit avro: ${type.identifier.value}")

    override fun emit(type: Type, ast: AST) = """
        |public record ${type.emitName()} (
        |${type.shape.emit()}
        |)${type.extends.run { if (isEmpty()) "" else " extends ${joinToString(", ") { it.emit() }}" }}${type.emitUnion(ast)} {
        |${emitTypeFunctionBody(type, ast)}
        |};
        |
    """.trimMargin()

    private fun emitTypeFunctionBody(type: Type, ast: AST) = """
        |  public static class Avro {
        |    
        |    public static final org.apache.avro.Schema SCHEMA = 
        |      new org.apache.avro.Schema.Parser().parse("${emitAvroSchema(type, ast)}");
        |
        |    public static ${type.emitName()} from(org.apache.avro.generic.GenericData.Record record) {
        |       return new ${type.emitName()}(
        |       ${type.shape.value.mapIndexed(emitFrom(ast)).joinToString(",\n${Spacer(4)}")}
        |      );
        |    }
        |    
        |    public static org.apache.avro.generic.GenericData.Record to(${type.emitName()} data) {
        |      var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
        |      ${type.shape.value.mapIndexed(emitTo).joinToString("\n${Spacer(3)}")}
        |      return record;
        |    }
        |  }
    """.trimMargin()

    override fun emit(enum: Enum, ast: AST) = """
        |public enum ${emit(enum.identifier)} implements Wirespec.Enum {
        |${enum.entries.joinToString(",\n") { "${it.sanitizeEnum().sanitizeKeywords()}(\"$it\")" }.spacer()};
        |${Spacer}public final String label;
        |${Spacer}${emit(enum.identifier)}(String label) {
        |${Spacer(2)}this.label = label;
        |${Spacer}}
        |${Spacer}@Override
        |${Spacer}public String toString() {
        |${Spacer(2)}return label;
        |${Spacer}}
        |${Spacer}@Override
        |${Spacer}public String getLabel() {
        |${Spacer(2)}return label;
        |${Spacer}}
        |${emitEnumFunctionBody(enum, ast)}
        |}
        |
    """.trimMargin()

     private fun emitEnumFunctionBody(enum: Enum, ast: AST) = """
        |  public static class Avro {
        |
        |    public static final org.apache.avro.Schema SCHEMA = 
        |      new org.apache.avro.Schema.Parser().parse("${emitAvroSchema(enum, ast)}");
        |    
        |    public static ${enum.emitName()} from(org.apache.avro.generic.GenericData.EnumSymbol record) {
        |      return ${enum.emitName()}.valueOf(record.toString());
        |    }
        |    
        |    public static org.apache.avro.generic.GenericData.EnumSymbol to(${enum.emitName()} data) {
        |      return new org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, data.name());
        |    }
        |  }
    """.trimMargin()

    private val emitTo:  (index: Int, field: Field) -> String = { index, field ->
        when (val reference = field.reference) {
            is Reference.Custom -> when {
                reference.isIterable -> "record.put(${index}, data.${emit(field.identifier)}().stream().map(it -> ${field.reference.value}.Avro.to(it)).toList());"
                else -> "record.put(${index}, ${field.reference.emit()}.Avro.to(data.${emit(field.identifier)}()));"
            }

            is Reference.Primitive -> when(reference.type) {
                is Reference.Primitive.Type.Bytes -> "record.put(${index}, java.nio.ByteBuffer.wrap(data.${emit(field.identifier)}()));"
                else -> "record.put(${index}, data.${emit(field.identifier)}()${if (field.isNullable) ".orElse(null)" else ""});"
            }

            else -> TODO()
        }
    }

    val emitFrom: (ast: AST) -> (index: Int, field: Field) -> String =
        { ast ->
            { index, field ->
                when (val reference = field.reference) {
                    is Reference.Custom -> when {
                        field.isNullable -> "(${field.emitType()}) java.util.Optional.ofNullable((${field.reference.emitType()}) record.get(${index}))"
                        reference.isIterable -> "((java.util.List<org.apache.avro.generic.GenericData.Record>) record.get(${index})).stream().map(it -> ${field.reference.emitType()}.Avro.from(it)).toList()"
                        reference.isEnum(ast) -> "${field.reference.emit()}.Avro.from((org.apache.avro.generic.GenericData.EnumSymbol) record.get(${index}))"
                        else -> "${field.reference.emit()}.Avro.from((org.apache.avro.generic.GenericData.Record) record.get(${index}))"
                    }

                    is Reference.Primitive -> when {
                        field.isNullable -> "(${field.emitType()}) java.util.Optional.ofNullable((${field.reference.emitType()}) record.get(${index}))"
                        reference.type == Reference.Primitive.Type.Bytes -> "(${field.emitType()}) ((java.nio.ByteBuffer) record.get(${index})).array()"
                        reference.type == Reference.Primitive.Type.String -> "(${field.emitType()}) record.get(${index}).toString()"
                        else -> "(${field.emitType()}) record.get(${index})"
                    }

                    else -> "(${field.emitType()}) record.get(${index})"
                }
            }
        }


}
