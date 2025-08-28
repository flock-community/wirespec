package community.flock.wirespec.integration.avro.java.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.spacer
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.integration.avro.Utils
import community.flock.wirespec.integration.avro.Utils.isEnum

class AvroEmitter(override val packageName: PackageName, emitShared: EmitShared) : JavaEmitter(packageName, emitShared) {

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = super.emit(ast, logger) + emitAvro<Type>(ast, ::emitAvroType) + emitAvro<Enum>(ast, ::emitAvroEnum)

    private fun emitAvroSchema(type: Definition, module: Module) = Utils.emitAvroSchema(packageName, type, module)
        ?.replace("\\\"<<<<<", "\" + ")
        ?.replace(">>>>>\\\"", "Avro.SCHEMA + \"")
        ?: error("Cannot emit avro: ${type.identifier}")

    private fun emitAvroType(type: Type, module: Module) = """
        |package ${packageName.value}.avro;
        |
        |import ${packageName.value}.model.${emit(type.identifier)};
        |
        |public class ${emit(type.identifier)}Avro {
        |  
        |  public static final org.apache.avro.Schema SCHEMA = 
        |    new org.apache.avro.Schema.Parser().parse("${emitAvroSchema(type, module)}");
        |
        |  public static ${emit(type.identifier)} from(org.apache.avro.generic.GenericData.Record record) {
        |    return new ${emit(type.identifier)}(
        |${type.shape.value.mapIndexed(emitFrom(module)).joinToString(",\n").spacer(3)}
        |    );
        |  }
        |  
        |  public static org.apache.avro.generic.GenericData.Record to(${emit(type.identifier)} data) {
        |    var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
        |${type.shape.value.mapIndexed(emitTo).joinToString("\n").spacer(3)}
        |    return record;
        |  }
        |}
    """.trimMargin()

    private fun emitAvroEnum(enum: Enum, module: Module) = """
        |package ${packageName.value}.avro;
        |
        |import ${packageName.value}.model.${emit(enum.identifier)};
        |
        |public class ${emit(enum.identifier)}Avro {
        |
        |  public static final org.apache.avro.Schema SCHEMA = 
        |    new org.apache.avro.Schema.Parser().parse("${emitAvroSchema(enum, module)}");
        |  
        |  public static ${emit(enum.identifier)} from(org.apache.avro.generic.GenericData.EnumSymbol record) {
        |    return ${emit(enum.identifier)}.valueOf(record.toString());
        |  }
        |  
        |  public static org.apache.avro.generic.GenericData.EnumSymbol to(${emit(enum.identifier)} data) {
        |    return new org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, data.name());
        |  }
        |}
    """.trimMargin()

    private val emitTo: (index: Int, field: Field) -> String = { index, field ->
        when (val reference = field.reference) {
            is Reference.Iterable -> "record.put($index, data.${emit(field.identifier)}().stream().map(it -> ${reference.reference.value.avroClass()}.to(it)).toList());"
            is Reference.Custom -> "record.put($index, ${field.reference.emit().avroClass()}.to(data.${emit(field.identifier)}()));"
            is Reference.Primitive -> when (reference.type) {
                is Reference.Primitive.Type.Bytes -> "record.put($index, java.nio.ByteBuffer.wrap(data.${emit(field.identifier)}()));"
                else -> "record.put($index, data.${emit(field.identifier)}()${if (reference.isNullable) ".orElse(null)" else ""});"
            }

            else -> TODO()
        }
    }

    private val emitFrom: (module: Module) -> (index: Int, field: Field) -> String =
        { module ->
            { index, field ->
                when (val reference = field.reference) {
                    is Reference.Iterable -> "((java.util.List<org.apache.avro.generic.GenericData.Record>) record.get($index)).stream().map(it -> ${reference.reference.emitRoot().avroClass()}.from(it)).toList()"
                    is Reference.Custom -> when {
                        reference.isNullable -> "(${reference.emit()}) java.util.Optional.ofNullable((${field.reference.emitRoot()}) record.get($index))"
                        reference.isEnum(module) -> "${field.reference.emit().avroClass()}.from((org.apache.avro.generic.GenericData.EnumSymbol) record.get($index))"
                        else -> "${field.reference.emit().avroClass()}.from((org.apache.avro.generic.GenericData.Record) record.get($index))"
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

    private inline fun <reified T : Definition> AvroEmitter.emitAvro(
        ast: AST,
        e: (t: T, module: Module) -> String,
    ): List<Emitted> = ast.modules.toList()
        .flatMap {
            it.statements.filterIsInstance<T>()
                .map { type -> Emitted("${packageName.toDir()}avro/${emit(type.identifier)}Avro.${extension.name}", e(type, it)) }
        }

    private fun String.avroClass(): String = replace(".model.", ".avro.") + "Avro"
}
