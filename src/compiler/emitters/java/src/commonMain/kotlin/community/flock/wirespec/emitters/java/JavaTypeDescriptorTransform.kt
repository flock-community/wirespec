package community.flock.wirespec.emitters.java

import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.Precision
import community.flock.wirespec.ir.core.RawExpression
import community.flock.wirespec.ir.core.Type
import community.flock.wirespec.ir.core.TypeDescriptor
import community.flock.wirespec.ir.core.transform
import community.flock.wirespec.ir.core.transformChildren

internal fun <T : Element> T.transformTypeDescriptors(): T = transform {
    statementAndExpression { stmt, tr ->
        when (stmt) {
            is TypeDescriptor -> {
                val rootType = stmt.type.findRoot()
                val containerStr = stmt.type.rawContainerClass()
                val rootStr = "${rootType.toJavaName()}.class"
                val containerArg = containerStr?.let { "$it.class" } ?: "null"
                RawExpression("Wirespec.getType($rootStr, $containerArg)")
            }
            else -> stmt.transformChildren(tr)
        }
    }
}

internal fun Type.findRoot(): Type = when (this) {
    is Type.Nullable -> type.findRoot()
    is Type.Array -> elementType.findRoot()
    is Type.Dict -> valueType.findRoot()
    else -> this
}

internal fun Type.rawContainerClass(): String? = when (this) {
    is Type.Nullable -> "java.util.Optional"
    is Type.Array -> "java.util.List"
    is Type.Dict -> "java.util.Map"
    else -> null
}

internal fun Type.toJavaName(): String = when (this) {
    is Type.Integer -> when (precision) {
        Precision.P32 -> "Integer"
        Precision.P64 -> "Long"
    }
    is Type.Number -> when (precision) {
        Precision.P32 -> "Float"
        Precision.P64 -> "Double"
    }
    Type.String -> "String"
    Type.Boolean -> "Boolean"
    Type.Bytes -> "byte[]"
    Type.Any -> "Object"
    Type.Unit -> "Void"
    is Type.Custom -> name.dotted()
    else -> "Object"
}
