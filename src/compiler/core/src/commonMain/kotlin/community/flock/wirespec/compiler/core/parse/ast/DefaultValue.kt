package community.flock.wirespec.compiler.core.parse.ast

public sealed interface DefaultValue {
    public data class StringValue(val value: String) : DefaultValue
    public data class IntegerValue(val value: String) : DefaultValue
    public data class NumberValue(val value: String) : DefaultValue
    public data class BooleanValue(val value: Boolean) : DefaultValue
    public data object NullValue : DefaultValue
}

/**
 * The default as it applies to a field of type [reference], or null when the field cannot take it:
 * the value has to match the field's type, precision and bounds. An integer is accepted on a
 * `Number` field and becomes a [DefaultValue.NumberValue] with a decimal point.
 */
public fun DefaultValue.coerceTo(reference: Reference): DefaultValue? = when (this) {
    DefaultValue.NullValue -> takeIf { reference.isNullable }
    else -> (reference as? Reference.Primitive)?.type?.let(::coerceTo)
}

private fun DefaultValue.coerceTo(type: Reference.Primitive.Type): DefaultValue? = when (type) {
    is Reference.Primitive.Type.String -> takeIf { it is DefaultValue.StringValue }
    is Reference.Primitive.Type.Boolean -> takeIf { it is DefaultValue.BooleanValue }
    is Reference.Primitive.Type.Integer -> (this as? DefaultValue.IntegerValue)
        ?.value?.toLongOrNull()
        ?.takeIf { it.fitsIn(type.precision) && type.constraint.admits(it.toDouble()) }
        ?.let { DefaultValue.IntegerValue(it.toString()) }

    is Reference.Primitive.Type.Number -> when (this) {
        is DefaultValue.IntegerValue -> value
        is DefaultValue.NumberValue -> value
        is DefaultValue.StringValue, is DefaultValue.BooleanValue, DefaultValue.NullValue -> null
    }
        ?.takeIf { value -> value.toDoubleOrNull()?.let(type.constraint::admits) ?: false }
        ?.let { value -> DefaultValue.NumberValue(if ('.' in value) value else "$value.0") }

    is Reference.Primitive.Type.Bytes -> null
}

private fun Long.fitsIn(precision: Reference.Primitive.Type.Precision) = when (precision) {
    Reference.Primitive.Type.Precision.P32 -> this in Int.MIN_VALUE..Int.MAX_VALUE
    Reference.Primitive.Type.Precision.P64 -> true
}

private fun Reference.Primitive.Type.Constraint.Bound?.admits(value: Double) = this == null ||
    (min?.toDouble()?.let { value >= it } ?: true) && (max?.toDouble()?.let { value <= it } ?: true)
