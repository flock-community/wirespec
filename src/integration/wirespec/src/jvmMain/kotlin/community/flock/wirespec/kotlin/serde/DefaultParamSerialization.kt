package community.flock.wirespec.kotlin.serde

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.serde.DefaultSerialization.deserializeEnum
import community.flock.wirespec.kotlin.serde.DefaultSerialization.deserializeList
import community.flock.wirespec.kotlin.serde.DefaultSerialization.deserializePrimitive
import community.flock.wirespec.kotlin.serde.DefaultSerialization.isList
import community.flock.wirespec.kotlin.serde.DefaultSerialization.isWirespecEnum
import kotlin.reflect.KType

public class DefaultParamSerialization : Wirespec.ParamSerialization {

    override fun <T : Any> serializeParam(value: T, type: KType): List<String> = when {
        type.isList() -> (value as List<*>).map { it.toString() }
        else -> listOf(value.toString())
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> deserializeParam(values: List<String>, type: KType): T = when {
        type.isList() -> deserializeList(values, type)
        type.isWirespecEnum() -> deserializeEnum(values, type)
        else -> deserializePrimitive(values, type)
    } as T
}
