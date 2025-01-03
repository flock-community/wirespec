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
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.javaType

@Configuration
@OptIn(ExperimentalStdlibApi::class)
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
open class WirespecSerializationConfiguration {

    companion object {
        private val PRIMITIVE_TYPES = mapOf<KClass<*>, (String) -> Any>(
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

        private fun isPrimitiveType(kClass: KClass<*>): Boolean = PRIMITIVE_TYPES.containsKey(kClass)
    }

    @Bean
    open fun wirespecSerialization(objectMapper: ObjectMapper) = object : Wirespec.Serialization<String> {
        private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModuleKotlin())

        override fun <T> serialize(t: T, kType: KType): String =
            when {
                t is String -> t
                else -> wirespecObjectMapper.writeValueAsString(t)
            }

        override fun <T> serializeQuery(name: String, value: T, kType: KType): Map<String, List<String>> {
            val classifier = kType.classifier

            if (classifier == List::class) {
                val elementType = kType.arguments.firstOrNull()?.type
                    ?: throw IllegalArgumentException("Cannot determine list element type")

                return if (isObjectType(elementType)) {
                    serializeObjectList(value as List<*>, elementType)
                } else {
                    val list = value as List<*>
                    val values = if (isWirespecEnum(elementType)) {
                        list.map { (it as Wirespec.Enum).label }
                    } else {
                        list.map { it.toString() }
                    }
                    mapOf(name to values)
                }
            }

            return if (isObjectType(kType)) {
                serializeObject(value)
            } else {
                val stringValue = if (isWirespecEnum(kType)) {
                    (value as Wirespec.Enum).label
                } else {
                    value.toString()
                }
                mapOf(name to listOf(stringValue))
            }
        }

        override fun <T> deserialize(raw: String, kType: KType): T =
            when {
                kType.classifier == String::class -> raw as T
                else -> wirespecObjectMapper
                    .constructType(kType.javaType)
                    .let { wirespecObjectMapper.readValue(raw, it) }
            }

        @Suppress("UNCHECKED_CAST")
        override fun <T> deserializeQuery(name: String, isNullable: Boolean, allQueryParams: Map<String, List<String>>, kType: KType): T {
            val classifier = kType.classifier as? KClass<*> ?: throw IllegalArgumentException("Invalid type classifier")

            // Handle null case first for non-object types
            if (!isObjectType(kType) && !allQueryParams.containsKey(name)) {
                if (isNullable) return null as T
                throw IllegalArgumentException("Required query parameter '$name' is missing")
            }

            return when {
                classifier == List::class || classifier == Array::class -> deserializeList(name, allQueryParams, kType)
                isObjectType(kType) -> deserializeObject(allQueryParams, kType)
                isWirespecEnum(kType) -> deserializeEnum(name, allQueryParams, classifier)
                else -> deserializePrimitive(name, allQueryParams, classifier)
            } as T
        }

        private fun deserializeList(name: String, allQueryParams: Map<String, List<String>>, kType: KType): Any {
            val elementType = kType.arguments.firstOrNull()?.type
                ?: throw IllegalArgumentException("Cannot determine list element type")

            return if (isObjectType(elementType)) {
                deserializeObjectList(allQueryParams, elementType)
            } else {
                val values = allQueryParams[name] ?: emptyList()
                when {
                    isWirespecEnum(elementType) -> deserializeEnumList(values, elementType)
                    else -> deserializePrimitiveList(values, elementType.classifier as KClass<*>)
                }
            }
        }

        private fun deserializePrimitive(name: String, params: Map<String, List<String>>, classifier: KClass<*>): Any {
            val value = params[name]?.firstOrNull()
                ?: throw IllegalArgumentException("Missing value for $name")
            return PRIMITIVE_TYPES[classifier]?.invoke(value)
                ?: throw IllegalArgumentException("Unsupported type: $classifier")
        }

        private fun deserializePrimitiveList(values: List<String>, classifier: KClass<*>): List<Any> {
            val converter = PRIMITIVE_TYPES[classifier]
                ?: throw IllegalArgumentException("Unsupported list element type: $classifier")
            return values.map(converter)
        }

        private fun deserializeEnum(name: String, params: Map<String, List<String>>, enumClass: KClass<*>): Any {
            val value = params[name]?.firstOrNull()
                ?: throw IllegalArgumentException("Missing enum value for $name")
            return findEnumByLabel(enumClass, value)
        }

        private fun deserializeEnumList(values: List<String>, elementType: KType): List<Any> {
            val enumClass = elementType.classifier as KClass<*>
            return values.map { findEnumByLabel(enumClass, it) }
        }

        private fun findEnumByLabel(enumClass: KClass<*>, label: String): Any {
            return enumClass.java.enumConstants.firstOrNull {
                (it as Wirespec.Enum).label == label
            } ?: throw IllegalArgumentException("Invalid enum value: $label")
        }

        private fun <T> deserializeObject(queryParams: Map<String, List<String>>, kType: KType): T {
            val kClass = kType.classifier as KClass<*>
            val constructor = kClass.primaryConstructor
                ?: throw IllegalArgumentException("Class ${kClass.simpleName} must have a primary constructor")

            val arguments = constructor.parameters.associate { param ->
                val paramType = param.type
                val paramName = param.name ?: throw IllegalArgumentException("Constructor parameter must have a name")
                val paramValue = queryParams[paramName]?.firstOrNull()

                val value = when {
                    paramValue == null -> {
                        if (paramType.isMarkedNullable) null
                        else throw IllegalArgumentException("Missing required non-nullable parameter: $paramName")
                    }
                    isWirespecEnum(paramType) -> findEnumByLabel(paramType.classifier as KClass<*>, paramValue)
                    else -> {
                        val classifier = paramType.classifier as? KClass<*>
                            ?: throw IllegalArgumentException("Invalid type classifier for parameter: $paramName")

                        PRIMITIVE_TYPES[classifier]?.invoke(paramValue)
                            ?: throw IllegalArgumentException("Unsupported parameter type: ${paramType.classifier}")
                    }
                }
                param to value
            }

            @Suppress("UNCHECKED_CAST")
            return constructor.callBy(arguments) as T
        }

        private fun deserializeObjectList(queryParams: Map<String, List<String>>, elementType: KType): List<Any> {
            val kClass = elementType.classifier as KClass<*>
            val constructor = kClass.primaryConstructor
                ?: throw IllegalArgumentException("Class ${kClass.simpleName} must have a primary constructor")

            val maxLength = queryParams.values.maxOfOrNull { it.size } ?: 0

            // Validate non-nullable parameters upfront
            validateNonNullableParameters(constructor, queryParams, maxLength)

            return (0 until maxLength).map { index ->
                val arguments = constructor.parameters.associate { param ->
                    val paramName = param.name ?: throw IllegalArgumentException("Constructor parameter must have a name")
                    val paramType = param.type
                    val paramValue = queryParams[paramName]?.getOrNull(index)

                    val value = when {
                        paramValue == null -> {
                            if (paramType.isMarkedNullable) null
                            else throw IllegalArgumentException("Missing required non-nullable parameter: $paramName at index $index")
                        }
                        isWirespecEnum(paramType) -> findEnumByLabel(paramType.classifier as KClass<*>, paramValue)
                        else -> {
                            val classifier = paramType.classifier as? KClass<*>
                                ?: throw IllegalArgumentException("Invalid type classifier for parameter: $paramName")

                            PRIMITIVE_TYPES[classifier]?.invoke(paramValue)
                                ?: throw IllegalArgumentException("Unsupported parameter type: ${paramType.classifier}")
                        }
                    }
                    param to value
                }
                constructor.callBy(arguments)
            }
        }

        private fun validateNonNullableParameters(
            constructor: kotlin.reflect.KFunction<Any>,
            queryParams: Map<String, List<String>>,
            maxLength: Int
        ) {
            (0 until maxLength).forEach { index ->
                constructor.parameters
                    .filter { !it.type.isMarkedNullable }
                    .forEach { param ->
                        val paramName = param.name ?: throw IllegalArgumentException("Constructor parameter must have a name")
                        val values = queryParams[paramName]
                        if (values == null || index >= values.size) {
                            throw IllegalArgumentException("Missing required non-nullable parameter: $paramName at index $index")
                        }
                    }
            }
        }

        private fun <T> serializeObject(value: T): Map<String, List<String>> {
            val kClass = value!!::class
            return kClass.memberProperties
                .filter { it.visibility == KVisibility.PUBLIC }
                .associate { prop ->
                    val propValue = prop.getter.call(value)
                    if (propValue != null) {
                        prop.name to listOf(propValue.toString())
                    } else {
                        prop.name to emptyList()
                    }
                }
                .filterValues { it.isNotEmpty() }
        }

        private fun serializeObjectList(values: List<*>, elementType: KType): Map<String, List<String>> {
            if (values.isEmpty()) return emptyMap()

            val kClass = (elementType.classifier as KClass<*>)
            val properties = kClass.memberProperties
                .filter { it.visibility == KVisibility.PUBLIC }

            return properties.associate { prop ->
                prop.name to values.mapNotNull { value ->
                    value?.let { prop.getter.call(it)?.toString() }
                }
            }.filterValues { it.isNotEmpty() }
        }
    }

    private fun isObjectType(type: KType): Boolean {
        val classifier = type.classifier as? KClass<*> ?: return false
        return !isPrimitiveType(classifier) && !classifier.supertypes.any { it.classifier == Wirespec.Enum::class }
    }

    private fun isWirespecEnum(type: KType): Boolean {
        val classifier = type.classifier as? KClass<*> ?: return false
        return classifier.supertypes.any { it.classifier == Wirespec.Enum::class }
    }
}