package community.flock.wirespec.compiler.core.parse.ast

public sealed interface DefaultValue {
    public data class StringValue(val value: String) : DefaultValue
    public data class IntegerValue(val value: String) : DefaultValue
    public data class NumberValue(val value: String) : DefaultValue
    public data class BooleanValue(val value: Boolean) : DefaultValue
    public data class EnumValue(val value: String) : DefaultValue
    public data object NullValue : DefaultValue
}

/**
 * The default as it applies to a field of type [reference], or null when the field cannot take it:
 * the value has to match the field's type, precision and bounds. An integer is accepted on a
 * `Number` field and becomes a [DefaultValue.NumberValue] with a decimal point.
 *
 * On a reference to another definition the default becomes a [DefaultValue.EnumValue]. Whether that
 * definition is an enum with this entry needs the other definitions, see [isEntryOf].
 */
public fun DefaultValue.coerceTo(reference: Reference): DefaultValue? = when (this) {
    is DefaultValue.NullValue -> takeIf { reference.isNullable }
    else -> when (reference) {
        is Reference.Primitive -> coerceTo(reference.type)
        is Reference.Custom -> toEnumValue()
        is Reference.Any, is Reference.Unit, is Reference.Dict, is Reference.Iterable -> null
    }
}

public fun DefaultValue.EnumValue.isEntryOf(reference: Reference, definitions: Iterable<Definition>): Boolean = reference is Reference.Custom &&
    definitions.any { it is Enum && it.identifier.value == reference.value && value in it.entries }

// Enum entries can be integers, such as `enum Code { 200, 404 }`.
private fun DefaultValue.toEnumValue(): DefaultValue.EnumValue? = when (this) {
    is DefaultValue.EnumValue -> this
    is DefaultValue.IntegerValue -> DefaultValue.EnumValue(value)
    is DefaultValue.StringValue, is DefaultValue.NumberValue, is DefaultValue.BooleanValue, is DefaultValue.NullValue -> null
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
        is DefaultValue.StringValue, is DefaultValue.BooleanValue, is DefaultValue.EnumValue, is DefaultValue.NullValue -> null
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
    (min?.toDouble()?.let { value >= it } ?: true) &&
    (max?.toDouble()?.let { value <= it } ?: true)
