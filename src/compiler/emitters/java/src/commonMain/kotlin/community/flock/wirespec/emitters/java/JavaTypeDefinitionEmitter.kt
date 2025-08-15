package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type

interface JavaTypeDefinitionEmitter: TypeDefinitionEmitter, IdentifierEmitter {

    override fun emit(type: Type, module: Module) = """
        |public record ${emit(type.identifier)} (
        |${type.shape.emit()}
        |)${type.extends.run { if (isEmpty()) "" else " implements ${joinToString(", ") { it.emit() }}" }} {
        |};
        |
    """.trimMargin()

    override fun Type.Shape.emit() = value.joinToString("\n") { "${Spacer}${it.emit()}," }.dropLast(1)

    override fun Field.emit() = "${reference.emit()} ${emit(identifier)}"

    override fun Reference.emit(): String = emitType()
        .let { if (isNullable) "java.util.Optional<$it>" else it }

    private fun Reference.emitType(): String = when (this) {
        is Reference.Dict -> "java.util.Map<String, ${reference.emit()}>"
        is Reference.Iterable -> "java.util.List<${reference.emit()}>"
        is Reference.Unit -> "void"
        is Reference.Any -> "Object"
        is Reference.Custom -> value
        is Reference.Primitive -> emit()
    }

    fun Reference?.emitRoot(void: String = "void"): String = when (this) {
        is Reference.Dict -> reference.emitRoot()
        is Reference.Iterable -> reference.emitRoot()
        is Reference.Unit -> void
        is Reference.Any -> emitType()
        is Reference.Custom -> emitType()
        is Reference.Primitive -> emitType()
        null -> void
    }

    private fun Reference.Primitive.emit() = when (val t = type) {
        is Reference.Primitive.Type.String -> "String"
        is Reference.Primitive.Type.Integer -> when (t.precision) {
            Reference.Primitive.Type.Precision.P32 -> "Integer"
            Reference.Primitive.Type.Precision.P64 -> "Long"
        }

        is Reference.Primitive.Type.Number -> when (t.precision) {
            Reference.Primitive.Type.Precision.P32 -> "Float"
            Reference.Primitive.Type.Precision.P64 -> "Double"
        }

        is Reference.Primitive.Type.Boolean -> "Boolean"
        is Reference.Primitive.Type.Bytes -> "byte[]"
    }

    override fun Refined.emitValidator():String {
        val defaultReturn = "true;"
        return when (val type = reference.type) {
            is Reference.Primitive.Type.Integer -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.Number -> type.constraint?.emit() ?: defaultReturn
            is Reference.Primitive.Type.String -> type.constraint?.emit() ?: defaultReturn
            Reference.Primitive.Type.Boolean -> defaultReturn
            Reference.Primitive.Type.Bytes -> defaultReturn
        }
    }

    override fun Reference.Primitive.Type.Constraint.emit() = when(this){
        is Reference.Primitive.Type.Constraint.RegExp -> """java.util.regex.Pattern.compile("${expression.replace("\\", "\\\\")}").matcher(record.value).find();"""
        is Reference.Primitive.Type.Constraint.Bound -> """$min < record.value && record.value < $max;"""
    }

}
