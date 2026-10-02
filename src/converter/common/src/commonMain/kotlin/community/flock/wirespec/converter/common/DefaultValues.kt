package community.flock.wirespec.converter.common

import community.flock.wirespec.compiler.core.parse.ast.DefaultValue
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.coerceTo
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

private val INTEGER = Regex("-?[0-9]+")
private val NUMBER = Regex("-?[0-9]+\\.[0-9]+")

/**
 * The default of a field of type [reference], read from a JSON schema default. Defaults the field
 * cannot take, such as objects, arrays or values of another type, are dropped.
 */
public fun JsonElement.toDefaultValue(reference: Reference): DefaultValue? = (this as? JsonPrimitive)
    ?.toDefaultValue()
    ?.coerceTo(reference)

private fun JsonPrimitive.toDefaultValue(): DefaultValue? = when {
    this is JsonNull -> DefaultValue.NullValue
    isString -> DefaultValue.StringValue(content)
    else -> content.toBooleanStrictOrNull()?.let(DefaultValue::BooleanValue)
        ?: content.takeIf(INTEGER::matches)?.let(DefaultValue::IntegerValue)
        ?: content.takeIf(NUMBER::matches)?.let(DefaultValue::NumberValue)
}

public fun DefaultValue.toJsonElement(): JsonElement = when (this) {
    is DefaultValue.StringValue -> JsonPrimitive(value)
    is DefaultValue.IntegerValue -> JsonPrimitive(value.toLong())
    is DefaultValue.NumberValue -> JsonPrimitive(value.toDouble())
    is DefaultValue.BooleanValue -> JsonPrimitive(value)
    DefaultValue.NullValue -> JsonNull
}
