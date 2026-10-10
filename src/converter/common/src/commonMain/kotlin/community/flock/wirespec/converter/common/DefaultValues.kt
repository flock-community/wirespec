package community.flock.wirespec.converter.common

import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.DefaultValue
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Part
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.core.parse.ast.coerceTo
import community.flock.wirespec.compiler.core.parse.ast.fields
import community.flock.wirespec.compiler.core.parse.ast.isEntryOf
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

private val INTEGER = Regex("-?[0-9]+")
private val NUMBER = Regex("-?[0-9]+\\.[0-9]+")

/**
 * The default of a field of type [reference], read from a JSON schema default. Defaults the field
 * cannot take, such as objects, arrays or values of another type, are dropped. A default on a
 * reference becomes an enum entry, to be checked with [withValidEnumDefaults].
 */
public fun JsonElement.toDefaultValue(reference: Reference): DefaultValue? = (this as? JsonPrimitive)
    ?.toDefaultValue()
    ?.let { if (it is DefaultValue.StringValue && reference is Reference.Custom) DefaultValue.EnumValue(it.value) else it }
    ?.coerceTo(reference)

/**
 * Drops every enum default that does not name an entry of the enum, among [definitions], that it refers to.
 */
public fun Definition.withValidEnumDefaults(definitions: Iterable<Definition>): Definition = when (this) {
    is Type -> copy(
        shape = Type.Shape(
            shape.value.fields.map { field ->
                when (val default = field.defaultValue) {
                    is DefaultValue.EnumValue -> field.takeIf { default.isEntryOf(field.reference, definitions) } ?: field.copy(defaultValue = null)
                    else -> field
                }
            },
        ),
    )

    is Endpoint, is Channel, is Rpc, is Enum, is Union, is Refined, is Part -> this
}

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
    is DefaultValue.EnumValue -> JsonPrimitive(value)
    is DefaultValue.NullValue -> JsonNull
}
