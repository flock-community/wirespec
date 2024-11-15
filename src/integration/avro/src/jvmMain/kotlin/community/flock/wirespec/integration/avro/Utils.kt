package community.flock.wirespec.integration.avro

import arrow.core.escaped
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.converter.avro.AvroEmitter
import community.flock.wirespec.converter.avro.AvroModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object Utils {

    fun emitAvroSchema(packageName:String, type: Definition, ast: AST) = AvroEmitter
        .emit(ast)
        .map {
            when (it) {
                is AvroModel.RecordType -> it.copy(namespace = packageName)
                else -> it
            }
        }
        .find {
            when (it) {
                is AvroModel.RecordType -> it.name == type.identifier.value
                is AvroModel.EnumType -> it.name == type.identifier.value
                else -> false
            }
        }
        ?.flatten()
        ?.let { Json.encodeToString(it) }
        ?.escaped()

    fun Reference.isEnum(ast: AST): Boolean {
        return when (this) {
            is Reference.Custom -> ast
                .filterIsInstance<Enum>()
                .any { it.identifier.value == this.value }

            is Reference.Any -> false
            is Reference.Primitive -> false
            is Reference.Unit -> false
        }
    }

    private fun AvroModel.Type.flatten(): AvroModel.Type =
        when (this) {
            is AvroModel.RecordType -> this
                .copy(
                    fields = fields
                        .map { field ->
                            field.copy(type = AvroModel.TypeList(field.type
                                .map { it.flatten() }
                            ))
                        }
                )

            is AvroModel.ArrayType -> this.copy(items = items.flatten())
            is AvroModel.EnumType -> this
            is AvroModel.LogicalType -> this
            is AvroModel.SimpleType -> this.copy(
                value = when (value) {
                    "boolean", "int", "long", "float", "double", "bytes", "string", "null" -> value
                    else -> "<<<<<$value>>>>>"
                },
            )
        }

}