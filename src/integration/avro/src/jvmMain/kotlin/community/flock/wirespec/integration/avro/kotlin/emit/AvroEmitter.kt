package community.flock.wirespec.integration.avro.kotlin.emit

import arrow.core.NonEmptyList
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.utils.Logger
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.integration.avro.Utils
import community.flock.wirespec.integration.avro.Utils.isEnum

class AvroEmitter(override val packageName: PackageName, emitShared: EmitShared) : KotlinEmitter(packageName, emitShared) {

    override fun emit(ast: AST, logger: Logger): NonEmptyList<Emitted> = super.emit(ast, logger) + emitAvro<Type>(ast, ::emitAvroType) + emitAvro<Enum>(ast, ::emitAvroEnum)

    private fun emitAvroSchema(type: Definition, module: Module) = Utils.emitAvroSchema(packageName, type, module)
        ?.replace("\\\"<<<<<", "\" + ")
        ?.replace(">>>>>\\\"", "Avro.SCHEMA + \"")
        ?: error("Cannot emit avro: ${type.identifier.value}")

    private fun emitAvroType(type: Type, module: Module) = """
        |package ${packageName.value}.avro
        |
        |import ${packageName.value}.model.${emit(type.identifier)}
        |
        |object ${emit(type.identifier)}Avro {
        |  val SCHEMA = org.apache.avro.Schema.Parser().parse("${emitAvroSchema(type, module)}")
        |
        |  @JvmStatic
        |  fun from(record: org.apache.avro.generic.GenericData.Record): ${emit(type.identifier)} {
        |    return ${emit(type.identifier)}(
        |      ${type.shape.value.mapIndexed(emitFrom(module)).joinToString(",\n${Spacer(5)}")}
        |    )
        |  }
        |
        |  @JvmStatic
        |  fun to(model: ${emit(type.identifier)} ): org.apache.avro.generic.GenericData.Record {
        |    val record = org.apache.avro.generic.GenericData.Record(SCHEMA)
        |    ${type.shape.value.mapIndexed(emitTo).joinToString("\n${Spacer(4)}")}
        |    return record
        |  }
        |
        |}
        |
    """.trimMargin()

    private fun emitAvroEnum(enum: Enum, module: Module) = """
        |package ${packageName.value}.avro
        |
        |import ${packageName.value}.model.${emit(enum.identifier)}
        |
        |object ${emit(enum.identifier)}Avro {
        |
        |  val SCHEMA: org.apache.avro.Schema = org.apache.avro.Schema.Parser().parse("${emitAvroSchema(enum, module)}")
        |
        |  @JvmStatic
        |  fun from(record: org.apache.avro.generic.GenericData.EnumSymbol): ${emit(enum.identifier)} {
        |    return ${emit(enum.identifier)}.valueOf(record.toString())
        |  }
        |
        |  @JvmStatic
        |  fun to(model: ${emit(enum.identifier)}): org.apache.avro.generic.GenericData.EnumSymbol {
        |    return org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, model.name)
        |  }
        |
        |}
        |
    """.trimMargin()

    private val emitTo: (index: Int, field: Field) -> String = { index, field ->
        when (val reference = field.reference) {
            is Reference.Iterable -> "record.put($index, model.${emit(field.identifier)}.map{${reference.reference.value.avroClass()}.to(it)})"
            is Reference.Custom -> "record.put($index, ${field.reference.emit().avroClass()}.to(model.${emit(field.identifier)}))"
            is Reference.Primitive -> when {
                reference.type == Reference.Primitive.Type.Bytes -> "record.put($index, java.nio.ByteBuffer.wrap(model.${emit(field.identifier)}.toByteArray()))"
                else -> "record.put($index, model.${emit(field.identifier)})"
            }
            else -> error("Cannot emit Avro: $reference")
        }
    }

    private val emitFrom: (module: Module) -> (index: Int, field: Field) -> String =
        { module ->
            { index, field ->
                when (val reference = field.reference) {
                    is Reference.Iterable -> "(record.get($index) as java.util.List<org.apache.avro.generic.GenericData.Record>).map{${reference.reference.emit().avroClass()}.from(it)}"
                    is Reference.Custom -> when {
                        reference.isEnum(module) -> "${field.reference.emit().avroClass()}.from(record.get($index) as org.apache.avro.generic.GenericData.EnumSymbol)"
                        else -> "${field.reference.emit().avroClass()}.from(record.get($index) as org.apache.avro.generic.GenericData.Record)"
                    }
                    is Reference.Primitive -> when (reference.type) {
                        is Reference.Primitive.Type.Bytes -> "String((record.get($index) as java.nio.ByteBuffer).array())"
                        is Reference.Primitive.Type.String -> "record.get($index)${if (reference.isNullable) "?" else ""}.toString() as ${reference.emit()}"
                        else -> "record.get($index) as ${reference.emit()}"
                    }
                    else -> "record.get($index): ${reference.emit()}"
                }
            }
        }

    private inline fun <reified T : Definition> AvroEmitter.emitAvro(
        ast: AST,
        e: (t: T, module: Module) -> String,
    ): List<Emitted> = ast.modules.toList()
        .flatMap {
            it.statements.filterIsInstance<T>()
                .map { type -> Emitted("${packageName.toDir()}avro/${emit(type.identifier)}Avro.${extension.value}", e(type, it)) }
        }

    private fun String.avroClass(): String = replace(".model.", ".avro.") + "Avro"
}
