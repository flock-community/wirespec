package community.flock.wirespec.integration.avro.java.emit

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.integration.avro.Utils
import community.flock.wirespec.integration.avro.Utils.isEnum

class AvroEmitter(private val packageName: PackageName, emitShared: EmitShared) : JavaEmitter(packageName, emitShared) {

    private fun emitAvroSchema(type: Definition, module: Module) = Utils.emitAvroSchema(packageName, type, module)
        ?.replace("\\\"<<<<<", "\" + ")
        ?.replace(">>>>>\\\"", ".Avro.SCHEMA + \"")
        ?: error("Cannot emit avro: ${type.identifier.value}")

    override fun emit(type: Type, module: Module) = """
        |public record ${emit(type.identifier)} (
        |${type.shape.emit()}
        |)${type.extends.run { if (isEmpty()) "" else " implements ${joinToString(", ") { it.emit() }}" }} {
        |${emitTypeFunctionBody(type, module)}
        |};
        |
    """.trimMargin()

    private fun emitTypeFunctionBody(type: Type, module: Module) = """
        |  public static class Avro {
        |    
        |    public static final org.apache.avro.Schema SCHEMA = 
        |      new org.apache.avro.Schema.Parser().parse("${emitAvroSchema(type, module)}");
        |
        |    public static ${emit(type.identifier)} from(org.apache.avro.generic.GenericData.Record record) {
        |       return new ${emit(type.identifier)}(
        |       ${type.shape.value.mapIndexed(emitFrom(module)).joinToString(",\n${Spacer(4)}")}
        |      );
        |    }
        |    
        |    public static org.apache.avro.generic.GenericData.Record to(${emit(type.identifier)} data) {
        |      var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
        |      ${type.shape.value.mapIndexed(emitTo).joinToString("\n${Spacer(3)}")}
        |      return record;
        |    }
        |  }
    """.trimMargin()

    override fun emit(enum: Enum, module: Module) = """
        |public enum ${emit(enum.identifier)} implements Wirespec.Enum {
        |${enum.entries.joinToString(",\n") { "${it.sanitizeEnum().sanitizeKeywords()}(\"$it\")" }.spacer()};
        |${Spacer}public final String label;
        |${Spacer}${emit(enum.identifier)}(String label) {
        |${Spacer(2)}this.label = label;
        |$Spacer}
        |$Spacer@Override
        |${Spacer}public String toString() {
        |${Spacer(2)}return label;
        |$Spacer}
        |$Spacer@Override
        |${Spacer}public String getLabel() {
        |${Spacer(2)}return label;
        |$Spacer}
        |${emitEnumFunctionBody(enum, module)}
        |}
        |
    """.trimMargin()

    private fun emitEnumFunctionBody(enum: Enum, module: Module) = """
        |  public static class Avro {
        |
        |    public static final org.apache.avro.Schema SCHEMA = 
        |      new org.apache.avro.Schema.Parser().parse("${emitAvroSchema(enum, module)}");
        |    
        |    public static ${emit(enum.identifier)} from(org.apache.avro.generic.GenericData.EnumSymbol record) {
        |      return ${emit(enum.identifier)}.valueOf(record.toString());
        |    }
        |    
        |    public static org.apache.avro.generic.GenericData.EnumSymbol to(${emit(enum.identifier)} data) {
        |      return new org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, data.name());
        |    }
        |  }
    """.trimMargin()

    private val emitTo: (index: Int, field: Field) -> String = { index, field ->
        when (val reference = field.reference) {
            is Reference.Iterable -> "record.put($index, data.${emit(field.identifier)}().stream().map(it -> ${reference.reference.value}.Avro.to(it)).toList());"
            is Reference.Custom -> "record.put($index, ${field.reference.emit()}.Avro.to(data.${emit(field.identifier)}()));"
            is Reference.Primitive -> when (reference.type) {
                is Reference.Primitive.Type.Bytes -> "record.put($index, java.nio.ByteBuffer.wrap(data.${emit(field.identifier)}()));"
                else -> "record.put($index, data.${emit(field.identifier)}()${if (reference.isNullable) ".orElse(null)" else ""});"
            }

            else -> TODO()
        }
    }

    val emitFrom: (module: Module) -> (index: Int, field: Field) -> String =
        { module ->
            { index, field ->
                when (val reference = field.reference) {
                    is Reference.Iterable -> "((java.util.List<org.apache.avro.generic.GenericData.Record>) record.get($index)).stream().map(it -> ${reference.reference.emitRoot()}.Avro.from(it)).toList()"
                    is Reference.Custom -> when {
                        reference.isNullable -> "(${reference.emit()}) java.util.Optional.ofNullable((${field.reference.emitRoot()}) record.get($index))"
                        reference.isEnum(module) -> "${field.reference.emit()}.Avro.from((org.apache.avro.generic.GenericData.EnumSymbol) record.get($index))"
                        else -> "${field.reference.emit()}.Avro.from((org.apache.avro.generic.GenericData.Record) record.get($index))"
                    }

                    is Reference.Primitive -> when {
                        reference.isNullable -> "(${reference.emit()}) java.util.Optional.ofNullable((${field.reference.emitRoot()}) record.get($index))"
                        reference.type == Reference.Primitive.Type.Bytes -> "(${reference.emit()}) ((java.nio.ByteBuffer) record.get($index)).array()"
                        reference.type == Reference.Primitive.Type.String(null) -> "(${reference.emit()}) record.get($index).toString()"
                        else -> "(${reference.emit()}) record.get($index)"
                    }

                    else -> "(${reference.emit()}) record.get($index)"
                }
            }
        }
}
