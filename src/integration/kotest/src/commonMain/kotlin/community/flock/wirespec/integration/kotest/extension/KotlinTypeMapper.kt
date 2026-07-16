package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.parse.ast.Reference

internal object KotlinTypeMapper {
    fun map(reference: Reference): String = when (reference) {
        is Reference.Primitive -> when (val t = reference.type) {
            is Reference.Primitive.Type.String -> "String"
            is Reference.Primitive.Type.Integer -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Int"
                Reference.Primitive.Type.Precision.P64 -> "Long"
            }
            is Reference.Primitive.Type.Number -> when (t.precision) {
                Reference.Primitive.Type.Precision.P32 -> "Float"
                Reference.Primitive.Type.Precision.P64 -> "Double"
            }
            Reference.Primitive.Type.Boolean -> "Boolean"
            Reference.Primitive.Type.Bytes -> "ByteArray"
        }.appendNullable(reference.isNullable)
        is Reference.Custom -> reference.value.appendNullable(reference.isNullable)
        is Reference.Iterable -> "List<${map(reference.reference)}>".appendNullable(reference.isNullable)
        is Reference.Dict -> "Map<String, ${map(reference.reference)}>".appendNullable(reference.isNullable)
        is Reference.Unit -> "Unit".appendNullable(reference.isNullable)
        is Reference.Any -> "Any".appendNullable(reference.isNullable)
    }

    private fun String.appendNullable(isNullable: Boolean): String = if (isNullable) "$this?" else this
}
