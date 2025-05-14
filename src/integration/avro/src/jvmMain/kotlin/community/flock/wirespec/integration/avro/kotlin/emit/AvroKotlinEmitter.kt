package community.flock.wirespec.integration.avro.kotlin.emit

import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.common.EmitShared
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.core.emit.common.Spacer
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.integration.avro.Utils
import community.flock.wirespec.integration.avro.Utils.isEnum
import community.flock.wirespec.compiler.core.parse.Module

class AvroKotlinEmitter(private val packageName: PackageName, emitShared: EmitShared) : KotlinEmitter(packageName, emitShared) {

    private fun emitAvroSchema(type: Definition, module: Module) = Utils.emitAvroSchema(packageName, type, module)
        ?.replace("\\\"<<<<<", "\" + ")
        ?.replace(">>>>>\\\"", ".Avro.SCHEMA + \"")
        ?: error("Cannot emit avro: ${type.identifier.value}")

    override fun emit(type: Type, module: Module) =
        if (type.shape.value.isEmpty()) "${Spacer}model object ${emit(type.identifier)}"
        else """
            |data class ${emit(type.identifier)}(
            |${type.shape.emit()}
            |)${type.extends.run { if (isEmpty()) "" else " : ${joinToString(", ") { it.emit() }}" }}
            |{
            |${emitAvro(type, module)}
            |}
            |
        """.trimMargin()

    private fun emitAvro(type: Type, module: Module) = """
        |  class Avro {
        |    companion object {
        |      val SCHEMA = org.apache.avro.Schema.Parser().parse("${emitAvroSchema(type, module)}");
        |
        |      @JvmStatic
        |      fun from(record: org.apache.avro.generic.GenericData.Record): ${emit(type.identifier)} {
        |        return ${emit(type.identifier)}(
        |          ${type.shape.value.mapIndexed(emitFrom(module)).joinToString(",\n${Spacer(5)}")}
        |        );
        |      }
        |
        |      @JvmStatic
        |      fun to(model: ${emit(type.identifier)} ): org.apache.avro.generic.GenericData.Record {
        |        val record = org.apache.avro.generic.GenericData.Record(SCHEMA);
        |        ${type.shape.value.mapIndexed(emitTo).joinToString("\n${Spacer(4)}")}
        |        return record;
        |      }
        |    }
        |  }
        |
    """.trimMargin()

    override fun emit(enum: Enum, module: Module) = """
        |enum class ${emit(enum.identifier)} (override val label: String): Wirespec.Enum {
        |${enum.entries.joinToString(",\n") { "${it.sanitizeEnum().sanitizeKeywords()}(\"$it\")" }.spacer()};
        |${Spacer}override fun toString(): String {
        |${Spacer(2)}return label
        |${Spacer}}
        |${emitAvro(enum, module)}
        |}
        |
    """.trimMargin()

    private fun emitAvro(enum: Enum, module: Module) = """
        |  class Avro {
        |    companion object {
        |
        |       val SCHEMA: org.apache.avro.Schema = org.apache.avro.Schema.Parser().parse("${emitAvroSchema(enum, module)}");
        |
        |       @JvmStatic
        |       fun from(record: org.apache.avro.generic.GenericData.EnumSymbol): ${emit(enum.identifier)} {
        |         return ${emit(enum.identifier)}.valueOf(record.toString());
        |       }
        |
        |       @JvmStatic
        |       fun to(model: ${emit(enum.identifier)}): org.apache.avro.generic.GenericData.EnumSymbol {
        |         return org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, model.name);
        |       }
        |     }
        |  }
        |
    """.trimMargin()

    private val emitTo: (index: Int, field: Field) -> String = { index, field ->
        when (val reference = field.reference) {
            is Reference.Iterable -> "record.put(${index}, model.${emit(field.identifier)}.map{${reference.reference.value}.Avro.to(it)});"
            is Reference.Custom -> "record.put(${index}, ${field.reference.emit()}.Avro.to(model.${emit(field.identifier)}));"
            is Reference.Primitive -> when {
                reference.type == Reference.Primitive.Type.Bytes -> "record.put(${index}, java.nio.ByteBuffer.wrap(model.${emit(field.identifier)}.toByteArray()));"
                else -> "record.put(${index}, model.${emit(field.identifier)});"
            }

            else -> TODO()
        }
    }

    val emitFrom: (module: Module) -> (index: Int, field: Field) -> String =
        { module ->
            { index, field ->
                when (val reference = field.reference) {
                    is Reference.Iterable -> "(record.get(${index}) as java.util.List<org.apache.avro.generic.GenericData.Record>).map{${reference.reference.emit()}.Avro.from(it)}"
                    is Reference.Custom -> when {
                        reference.isEnum(module) -> "${field.reference.emit()}.Avro.from(record.get(${index}) as org.apache.avro.generic.GenericData.EnumSymbol)"
                        else -> "${field.reference.emit()}.Avro.from(record.get(${index}) as org.apache.avro.generic.GenericData.Record)"
                    }

                    is Reference.Primitive -> when (reference.type) {
                        Reference.Primitive.Type.Bytes -> "String((record.get(${index}) as java.nio.ByteBuffer).array())"
                        Reference.Primitive.Type.String -> "record.get(${index}).toString() as ${reference.emit()}"
                        else -> "record.get(${index}) as ${reference.emit()}"
                    }

                    else -> "record.get(${index}): ${reference.emit()}"
                }
            }
        }


}
