package community.flock.wirespec.kotlin.serde

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.serde.DefaultSerialization.deserializeEnum
import community.flock.wirespec.kotlin.serde.DefaultSerialization.deserializeList
import community.flock.wirespec.kotlin.serde.DefaultSerialization.deserializePrimitive
import community.flock.wirespec.kotlin.serde.DefaultSerialization.isList
import community.flock.wirespec.kotlin.serde.DefaultSerialization.isWirespecEnum
import kotlin.reflect.KType

class DefaultParamSerialization : Wirespec.ParamSerialization {

    override fun <T : Any> serializeParam(value: T, kType: KType): List<String> = when {
        kType.isList() -> (value as List<*>).map { it.toString() }
        else -> listOf(value.toString())
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> deserializeParam(values: List<String>, kType: KType): T = when {
        kType.isList() -> deserializeList(values, kType)
        kType.isWirespecEnum() -> deserializeEnum(values, kType)
        else -> deserializePrimitive(values, kType)
    } as T
}
