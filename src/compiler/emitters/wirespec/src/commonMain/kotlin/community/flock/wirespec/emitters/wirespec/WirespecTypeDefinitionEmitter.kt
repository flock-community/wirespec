package community.flock.wirespec.emitters.wirespec

import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type

interface WirespecTypeDefinitionEmitter: TypeDefinitionEmitter, IdentifierEmitter {

    override fun emit(type: Type, module: Module) = """
        |type ${emit(type.identifier)} {
        |${type.shape.emit()}
        |}
        |""".trimMargin()

    override fun Type.Shape.emit() = value.joinToString(",\n") { "$Spacer${it.emit()}" }

    override fun Field.emit() = "${emit(identifier)}: ${reference.emit()}"

    override fun Reference.emit(): String = when (this) {
        is Reference.Dict -> "{ ${reference.emit()} }"
        is Reference.Iterable -> "${reference.emit()}[]"
        is Reference.Unit -> "Unit"
        is Reference.Any -> "Any"
        is Reference.Custom -> value
        is Reference.Primitive -> when (val t = type) {
            is Reference.Primitive.Type.String -> "String"
            is Reference.Primitive.Type.Boolean -> "Boolean"
            is Reference.Primitive.Type.Bytes -> "Bytes"
            is Reference.Primitive.Type.Integer -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Integer32"
                Reference.Primitive.Type.Precision.P64 -> "Integer"
            }

            is Reference.Primitive.Type.Number -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Number32"
                Reference.Primitive.Type.Precision.P64 -> "Number"
            }
        }
    }.let { if (isNullable) "$it?" else it }

    override fun Refined.emitValidator():String {
        return when (val type = reference.type) {
            is Reference.Primitive.Type.Integer -> type.constraint?.emit() ?: ""
            is Reference.Primitive.Type.Number -> type.constraint?.emit() ?: ""
            is Reference.Primitive.Type.String -> type.constraint?.emit() ?: ""
            Reference.Primitive.Type.Boolean -> ""
            Reference.Primitive.Type.Bytes -> ""
        }
    }

}