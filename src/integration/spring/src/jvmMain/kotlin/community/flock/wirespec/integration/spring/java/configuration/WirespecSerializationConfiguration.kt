package community.flock.wirespec.integration.spring.java.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.jackson.java.WirespecModuleJava
import community.flock.wirespec.integration.spring.java.web.WirespecResponseBodyAdvice
import community.flock.wirespec.java.Wirespec
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
open class WirespecSerializationConfiguration {

    private val primitiveTypesConversion = mapOf<Class<*>, (String) -> Any>(
        String::class.java to { this },
        Int::class.javaObjectType to String::toInt,
        Long::class.javaObjectType to String::toLong,
        Double::class.javaObjectType to String::toDouble,
        Float::class.javaObjectType to String::toFloat,
        Boolean::class.javaObjectType to String::toBoolean,
        Char::class.javaObjectType to String::single,
        Byte::class.javaObjectType to String::toByte,
        Short::class.javaObjectType to String::toShort
    )

    @Bean
    open fun wirespecSerialization(objectMapper: ObjectMapper) = object : Wirespec.Serialization<String> {
        private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModuleJava())

        override fun <T> serialize(body: T, type: Type?): String = when {
            body is String -> body
            else -> wirespecObjectMapper.writeValueAsString(body)
        }

        override fun <T> serializeQuery(value: T?, type: Type?): List<String>? = when {
            value == null -> null
            isIterable(type) -> (value as Iterable<*>).map { it.toString() }
            else -> listOf(value.toString())
        }

        override fun <T : Any> deserialize(raw: String?, valueType: Type?): T? = raw?.let {
            when {
                valueType == String::class.java -> {
                    @Suppress("UNCHECKED_CAST")
                    raw as T
                }
                else -> {
                    val type = wirespecObjectMapper.constructType(valueType)
                    wirespecObjectMapper.readValue(it, type)
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> deserializeQuery(values: List<String>?, type: Type?): T? = when {
            values.isNullOrEmpty() -> null
            isIterable(type) -> deserializeList(values, getIterableElementType(type))
            isWirespecEnum(type) -> deserializeEnum(values, type as Class<*>)
            else -> deserializePrimitive(values, type as Class<*>)
        } as T?

        private fun deserializeList(values: List<String>, type: Type?): List<Any> = when {
            isWirespecEnum(type) -> values.map { findEnumByLabel(type as Class<*>, it) }
            else -> deserializePrimitiveList(values, type as Class<*>)
        }

        private fun deserializePrimitive(values: List<String>, clazz: Class<*>): Any {
            val value = values.firstOrNull()
                ?: throw IllegalArgumentException("No value provided for type: ${clazz.simpleName}")
            return primitiveTypesConversion[clazz]?.invoke(value)
                ?: throw IllegalArgumentException("Unsupported primitive type: ${clazz.simpleName}")
        }

        private fun deserializePrimitiveList(values: List<String>, clazz: Class<*>): List<Any> {
            val converter = primitiveTypesConversion[clazz]
                ?: throw IllegalArgumentException("Unsupported list element type: ${clazz.simpleName}")
            return values.map(converter)
        }

        private fun deserializeEnum(values: List<String>, enumClass: Class<*>): Any {
            val value = values.firstOrNull()
                ?: throw IllegalArgumentException("No enum value provided for type: ${enumClass.simpleName}")
            return findEnumByLabel(enumClass, value)
        }

        private fun findEnumByLabel(enumClass: Class<*>, label: String): Any =
            enumClass.enumConstants.firstOrNull {
                (it as Wirespec.Enum).label == label
            } ?: throw IllegalArgumentException("Invalid enum value '$label' for type: ${enumClass.simpleName}")

        private fun isIterable(type: Type?): Boolean =
            type is ParameterizedType && Iterable::class.java.isAssignableFrom(type.rawType as Class<*>)

        private fun isWirespecEnum(type: Type?): Boolean = when (type) {
            is Class<*> -> type.interfaces.contains(Wirespec.Enum::class.java)
            else -> false
        }

        private fun getIterableElementType(type: Type?): Type? =
            (type as? ParameterizedType)?.actualTypeArguments?.firstOrNull()
    }
}