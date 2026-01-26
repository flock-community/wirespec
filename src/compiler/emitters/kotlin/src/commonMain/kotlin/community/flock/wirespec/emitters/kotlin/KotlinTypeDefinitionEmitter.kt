package community.flock.wirespec.emitters.kotlin

import community.flock.wirespec.compiler.core.emit.Spacer
import community.flock.wirespec.compiler.core.emit.TypeDefinitionEmitter
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type

interface KotlinTypeDefinitionEmitter : TypeDefinitionEmitter, KotlinIdentifierEmitter {

    override fun emit(type: Type, module: Module) =
        if (type.shape.value.isEmpty()) "data object ${emit(type.identifier)}"
        else """
            |data class ${emit(type.identifier)}(
            |${type.shape.emit()}
            |)${type.extends.run { if (isEmpty()) "" else " : ${joinToString(", ") { it.emit() }}" }}
            |
        """.trimMargin()

    override fun Type.Shape.emit() = value.joinToString("\n") { "${Spacer}val ${it.emit()}," }.dropLast(1)

    override fun Field.emit() = "${emit(identifier)}: ${reference.emit()}"

    override fun Reference.emit(): String = when (this) {
        is Reference.Dict -> "Map<String, ${reference.emit()}>"
        is Reference.Iterable -> "List<${reference.emit()}>"
        is Reference.Unit -> "Unit"
        is Reference.Any -> "Any"
        is Reference.Custom -> value
        is Reference.Primitive -> when (val t = type) {
            is Reference.Primitive.Type.String -> "String"
            is Reference.Primitive.Type.Integer -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Int"
                Reference.Primitive.Type.Precision.P64 -> "Long"
            }

            is Reference.Primitive.Type.Number -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Float"
                Reference.Primitive.Type.Precision.P64 -> "Double"
            }

            is Reference.Primitive.Type.Boolean -> "Boolean"
            is Reference.Primitive.Type.Bytes -> "ByteArray"
        }
    }.let { if (isNullable) "$it?" else it }

    override fun Reference.Primitive.Type.Constraint.emit() = when (this) {
        is Reference.Primitive.Type.Constraint.RegExp -> "Regex(\"\"\"$expression\"\"\").matches(value)"
        is Reference.Primitive.Type.Constraint.Bound -> {
            val minCheck = min?.let { "$it < value" }
            val maxCheck = max?.let { "value < $it" }
            listOfNotNull(minCheck, maxCheck).joinToString(" && ").let { it.ifEmpty { "true" } }
        }
    }
}
