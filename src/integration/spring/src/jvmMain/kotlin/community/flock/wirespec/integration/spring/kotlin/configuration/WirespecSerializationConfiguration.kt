package community.flock.wirespec.integration.spring.kotlin.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.jackson.kotlin.WirespecModuleKotlin
import community.flock.wirespec.integration.spring.kotlin.web.WirespecResponseBodyAdvice
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.javaType

@Configuration
@OptIn(ExperimentalStdlibApi::class)
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
open class WirespecSerializationConfiguration {

    private val primitiveTypesConversion = mapOf<KClass<*>, (String) -> Any>(
        String::class to { this },
        Int::class to String::toInt,
        Long::class to String::toLong,
        Double::class to String::toDouble,
        Float::class to String::toFloat,
        Boolean::class to String::toBoolean,
        Char::class to String::single,
        Byte::class to String::toByte,
        Short::class to String::toShort
    )

    @Bean
    open fun wirespecSerialization(objectMapper: ObjectMapper) = object : Wirespec.Serialization<String> {
        private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModuleKotlin())

        override fun <T> serialize(t: T, kType: KType): String = when (t) {
            is String -> t
            else -> wirespecObjectMapper.writeValueAsString(t)
        }

        override fun <T> serializeQuery(value: T, kType: KType): List<String> = when {
            kType.isList() -> (value as List<*>).map { it.toString() }
            else -> listOf(value.toString())
        }

        override fun <T> deserialize(raw: String, kType: KType): T = when {
            kType.classifier == String::class -> raw as T
            else -> wirespecObjectMapper
                .constructType(kType.javaType)
                .let { wirespecObjectMapper.readValue(raw, it) }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> deserializeQuery(values: List<String>, kType: KType): T = when {
            kType.isList() -> deserializeList(values, kType)
            kType.isWirespecEnum() -> deserializeEnum(values, kType.classifierAsKClass())
            else -> deserializePrimitive(values, kType.classifierAsKClass())
        } as T

        private fun deserializeList(values: List<String>, kType: KType): List<Any> {
            val elementType = kType.getListElementType()
            return when {
                elementType.isWirespecEnum() -> values.map { findEnumByLabel(elementType.classifierAsKClass(), it) }
                else -> deserializePrimitiveList(values, elementType.classifierAsKClass())
            }
        }
    }

    private fun deserializePrimitive(values: List<String>, classifier: KClass<*>): Any {
        val value = values.firstOrNull()
            ?: throw IllegalArgumentException("No value provided for type: ${classifier.simpleName}")
        return primitiveTypesConversion[classifier]?.invoke(value)
            ?: throw IllegalArgumentException("Unsupported primitive type: ${classifier.simpleName}")
    }

    private fun deserializePrimitiveList(values: List<String>, classifier: KClass<*>): List<Any> {
        val converter = primitiveTypesConversion[classifier]
            ?: throw IllegalArgumentException("Unsupported list element type: ${classifier.simpleName}")
        return values.map(converter)
    }

    private fun deserializeEnum(values: List<String>, enumClass: KClass<*>): Any {
        val value = values.firstOrNull()
            ?: throw IllegalArgumentException("No enum value provided for type: ${enumClass.simpleName}")
        return findEnumByLabel(enumClass, value)
    }

    private fun findEnumByLabel(enumClass: KClass<*>, label: String): Any =
        enumClass.java.enumConstants.firstOrNull {
            (it as Wirespec.Enum).label == label
        } ?: throw IllegalArgumentException("Invalid enum value '$label' for type: ${enumClass.simpleName}")

    private fun KType.isList() = (classifier as? KClass<*>) == List::class

    private fun KType.isWirespecEnum() =
        (classifier as? KClass<*>)?.supertypes?.any { it.classifier == Wirespec.Enum::class } == true

    private fun KType.getListElementType() = arguments.single().type
        ?: throw IllegalArgumentException("Cannot determine list element type")

    private fun KType.classifierAsKClass(): KClass<*> =
        classifier as? KClass<*> ?: throw IllegalArgumentException("Invalid type classifier")
}