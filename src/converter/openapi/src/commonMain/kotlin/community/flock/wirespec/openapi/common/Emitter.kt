package community.flock.wirespec.openapi.common

import community.flock.wirespec.compiler.core.parse.ast.Reference

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

        else -> null
    }

    else -> null
}
