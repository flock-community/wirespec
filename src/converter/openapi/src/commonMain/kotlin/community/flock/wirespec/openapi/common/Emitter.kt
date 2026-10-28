package community.flock.wirespec.openapi.common

import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.StringFormat

internal fun Reference.emitFormat() = when (this) {
    is Reference.Primitive -> when (val t = type) {
        is Reference.Primitive.Type.Number -> when (t.precision) {
            Reference.Primitive.Type.Precision.P32 -> "float"
            Reference.Primitive.Type.Precision.P64 -> "double"
        }

        is Reference.Primitive.Type.Integer -> when (t.precision) {
            Reference.Primitive.Type.Precision.P32 -> "int32"
            Reference.Primitive.Type.Precision.P64 -> "int64"
        }

        is Reference.Primitive.Type.Bytes -> "binary"

        is Reference.Primitive.Type.String -> t.constraint?.emitFormat()

        else -> null
    }

    else -> null
}

internal fun Reference.Primitive.Type.Constraint.RegExp.emitFormat(): String? = StringFormat.of(this)?.label

/** An OpenAPI pattern is a regex without the delimiters and flags of a Wirespec one; a format has none. */
internal fun Reference.Primitive.Type.Constraint.RegExp.emitPattern(): String? = takeIf { StringFormat.of(it) == null }
    ?.value
    ?.substringAfter('/')
    ?.substringBeforeLast('/')
    ?.let { pattern -> Regex("\\\\.").replace(pattern) { if (it.value == "\\/") "/" else it.value } }
