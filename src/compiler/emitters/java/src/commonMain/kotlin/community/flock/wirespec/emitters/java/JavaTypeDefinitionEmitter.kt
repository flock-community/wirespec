package community.flock.wirespec.emitters.java

import community.flock.wirespec.compiler.core.emit.IdentifierEmitter
import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type

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

    override fun Reference.Primitive.Type.Constraint.emit() = when(this){
        is Reference.Primitive.Type.Constraint.RegExp -> """return java.util.regex.Pattern.compile("${expression.replace("\\", "\\\\")}").matcher(record.value).find();"""
        is Reference.Primitive.Type.Constraint.Bound -> {
            val minCheck = min?.let { "$it < record.value" }
            val maxCheck = max?.let { "record.value < $it" }
            val checks = listOfNotNull(minCheck, maxCheck).joinToString(" && ")
            """return ${if (checks.isEmpty()) "true" else checks};"""
        }
    }

}
