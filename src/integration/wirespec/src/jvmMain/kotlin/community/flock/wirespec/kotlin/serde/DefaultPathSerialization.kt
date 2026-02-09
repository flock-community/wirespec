package community.flock.wirespec.kotlin.serde

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.serde.DefaultSerialization.classifierAsKClass
import community.flock.wirespec.kotlin.serde.DefaultSerialization.findEnumByLabel
import community.flock.wirespec.kotlin.serde.DefaultSerialization.isWirespecEnum
import community.flock.wirespec.kotlin.serde.DefaultSerialization.isWirespecRefined
import community.flock.wirespec.kotlin.serde.DefaultSerialization.primitiveTypesConversion
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

class DefaultPathSerialization : Wirespec.PathSerialization {
    override fun <T> serializePath(t: T, kType: KType): String = when (t) {
        is Wirespec.Refined<*> -> t.value.toString()
        is Wirespec.Enum -> t.label
        else -> t.toString()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> deserializePath(value: String, kType: KType): T = when {
        kType.isWirespecRefined() -> {
            val kClass = kType.classifierAsKClass()
            val constructor = kClass.primaryConstructor
                ?: error("No primary constructor found for refined type: ${kClass.simpleName}")
            val paramType = constructor.parameters.first().type.classifier as KClass<*>
            val convertedValue = primitiveTypesConversion[paramType]?.invoke(value)
                ?: error("Unsupported refined value type: ${paramType.simpleName}")
            constructor.call(convertedValue)
        }
        kType.isWirespecEnum() -> findEnumByLabel(kType.classifierAsKClass(), value)
        else -> primitiveTypesConversion[kType.classifierAsKClass()]?.invoke(value)
    } as T
}
