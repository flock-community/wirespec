package community.flock.wirespec.kotlin.serde

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.KClass
import kotlin.reflect.KType

object DefaultSerialization {

    internal val primitiveTypesConversion = mapOf<KClass<*>, (String) -> Any>(
        String::class to { this },
        Int::class to String::toInt,
        Long::class to String::toLong,
        Double::class to String::toDouble,
        Float::class to String::toFloat,
        Boolean::class to String::toBoolean,
        Char::class to String::single,
        Byte::class to String::toByte,
        Short::class to String::toShort,
    )

    internal fun deserializeList(values: List<String>, kType: KType): List<Any> {
        val elementType = kType.getListElementType()
        return when {
            elementType.isWirespecEnum() -> values.map { findEnumByLabel(elementType.classifierAsKClass(), it) }
            else -> deserializePrimitiveList(values, elementType.classifierAsKClass())
        }
    }

    internal fun deserializePrimitive(values: List<String>, kType: KType): Any {
        val classifier = kType.classifierAsKClass()
        val value = values.firstOrNull()
            ?: error("No value provided for type: ${classifier.simpleName}")
        return primitiveTypesConversion[classifier]?.invoke(value)
            ?: error("Unsupported primitive type: ${classifier.simpleName}")
    }

    internal fun deserializePrimitiveList(values: List<String>, classifier: KClass<*>): List<Any> {
        val converter = primitiveTypesConversion[classifier]
            ?: error("Unsupported list element type: ${classifier.simpleName}")
        return values.map(converter)
    }

    internal fun deserializeEnum(values: List<String>, kType: KType): Any {
        val enumClass = kType.classifierAsKClass()
        val value = values.firstOrNull() ?: error("No enum value provided for type: ${enumClass.simpleName}")
        return findEnumByLabel(enumClass, value)
    }

    internal fun findEnumByLabel(enumClass: KClass<*>, label: String): Any = enumClass.java.enumConstants.firstOrNull {
        (it as Wirespec.Enum).label == label
    } ?: error("Invalid enum value '$label' for type: ${enumClass.simpleName}")

    internal fun KType.isList() = (classifier as? KClass<*>) == List::class

    internal fun KType.isWirespecEnum() = (classifier as? KClass<*>)?.supertypes?.any { it.classifier == Wirespec.Enum::class } == true

    internal fun KType.isWirespecRefined() = (classifier as? KClass<*>)?.supertypes?.any { it.classifier == Wirespec.Refined::class } == true

    internal fun KType.getListElementType() = arguments.single().type
        ?: error("Cannot determine list element type")

    internal fun KType.classifierAsKClass(): KClass<*> = classifier as? KClass<*> ?: error("Invalid type classifier")
}
