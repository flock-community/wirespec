package community.flock.wirespec.kotlin.serde

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.serde.DefaultSerialization.classifierAsKClass
import community.flock.wirespec.kotlin.serde.DefaultSerialization.findEnumByLabel
import community.flock.wirespec.kotlin.serde.DefaultSerialization.isWirespecEnum
import community.flock.wirespec.kotlin.serde.DefaultSerialization.primitiveTypesConversion
import kotlin.reflect.KType

open class DefaultPathSerialization : Wirespec.PathSerialization {
    override fun <T> serializePath(t: T, kType: KType): String = when {
        else -> t.toString()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> deserializePath(value: String, kType: KType): T = when {
        kType.isWirespecEnum() -> findEnumByLabel(kType.classifierAsKClass(), value)
        else -> primitiveTypesConversion[kType.classifierAsKClass()]?.invoke(value)
    } as T
}
