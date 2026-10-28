package community.flock.wirespec.converter.asyncapi

import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive.Type.Precision
import community.flock.wirespec.compiler.core.parse.ast.StringFormat

/** A reference as written in Wirespec, such as `Todo`, `String?[]`, `String?(uuid)` or `{ Integer }`, to name a type in an annotation. */
internal fun Reference.toWirespec(): String = when (this) {
    is Reference.Any -> "Any"
    is Reference.Unit -> "Unit"
    is Reference.Custom -> value
    is Reference.Dict -> "{ ${reference.toWirespec()} }"
    is Reference.Iterable -> "${reference.toWirespec()}[]"
    is Reference.Primitive -> when (val type = type) {
        is Reference.Primitive.Type.String -> "String"
        is Reference.Primitive.Type.Boolean -> "Boolean"
        is Reference.Primitive.Type.Bytes -> "Bytes"
        is Reference.Primitive.Type.Integer -> if (type.precision == Precision.P32) "Integer32" else "Integer"
        is Reference.Primitive.Type.Number -> if (type.precision == Precision.P32) "Number32" else "Number"
    }
} + (if (isNullable) "?" else "") + emitFormat()

// As in Wirespec, the format of a String comes after its `?`.
private fun Reference.emitFormat(): String = ((this as? Reference.Primitive)?.type as? Reference.Primitive.Type.String)
    ?.constraint
    ?.let(StringFormat::of)
    ?.let { "(${it.label})" }
    .orEmpty()

/** Reads a reference written by [toWirespec], from the right: the suffixes `?` and `[]` wrap what comes before them. */
internal fun String.toReference(): Reference = trim().let { source ->
    when {
        source.endsWith("?") -> source.dropLast(1).toReference().copy(isNullable = true)
        source.endsWith("[]") -> Reference.Iterable(source.dropLast(2).toReference(), isNullable = false)
        source.startsWith("{") && source.endsWith("}") -> Reference.Dict(source.drop(1).dropLast(1).toReference(), isNullable = false)
        source.endsWith(")") -> Reference.Primitive(
            type = Reference.Primitive.Type.String(StringFormat.of(source.substringAfter('(').dropLast(1).trim())?.regExp),
            isNullable = source.substringBefore('(').trim().endsWith("?"),
        )
        else -> when (source) {
            "Any" -> Reference.Any(isNullable = false)
            "Unit" -> Reference.Unit(isNullable = false)
            "String" -> Reference.Primitive(Reference.Primitive.Type.String(null), isNullable = false)
            "Boolean" -> Reference.Primitive(Reference.Primitive.Type.Boolean, isNullable = false)
            "Bytes" -> Reference.Primitive(Reference.Primitive.Type.Bytes, isNullable = false)
            "Integer" -> Reference.Primitive(Reference.Primitive.Type.Integer(Precision.P64, null), isNullable = false)
            "Integer32" -> Reference.Primitive(Reference.Primitive.Type.Integer(Precision.P32, null), isNullable = false)
            "Number" -> Reference.Primitive(Reference.Primitive.Type.Number(Precision.P64, null), isNullable = false)
            "Number32" -> Reference.Primitive(Reference.Primitive.Type.Number(Precision.P32, null), isNullable = false)
            else -> Reference.Custom(source, isNullable = false)
        }
    }
}
