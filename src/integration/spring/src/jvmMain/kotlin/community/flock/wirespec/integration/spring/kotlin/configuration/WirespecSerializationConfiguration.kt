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
                    // Handle list of Wirespec.Enum
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
                // Handle single Wirespec.Enum
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

            val classifier = kType.classifier

            // Handle null case first for non-object types
            if (!isObjectType(kType) && !allQueryParams.containsKey(name)) {
                if (isNullable) {
                    @Suppress("UNCHECKED_CAST")
                    return null as T
                } else {
                    throw IllegalArgumentException("Required query parameter '$name' is missing")
                }
            }

            @Suppress("UNCHECKED_CAST")
            return when {
                // Handle Lists and Arrays
                classifier == List::class || classifier == Array::class -> {
                    val elementType = kType.arguments.firstOrNull()?.type
                        ?: throw IllegalArgumentException("Cannot determine list element type")

                    if (isObjectType(elementType)) {
                        deserializeObjectList(allQueryParams, elementType) as T
                    } else {
                        val values = allQueryParams[name] ?: emptyList()
                        when {
                            isWirespecEnum(elementType) -> {
                                val enumClass = elementType.classifier as KClass<*>
                                values.map { enumValue ->
                                    enumClass.java.enumConstants.firstOrNull {
                                        (it as Wirespec.Enum).label == enumValue
                                    } ?: throw IllegalArgumentException("Invalid enum value: $enumValue")
                                } as T
                            }
                            elementType.classifier == String::class -> values as T
                            elementType.classifier == Int::class -> values.map { it.toInt() } as T
                            elementType.classifier == Long::class -> values.map { it.toLong() } as T
                            elementType.classifier == Double::class -> values.map { it.toDouble() } as T
                            elementType.classifier == Float::class -> values.map { it.toFloat() } as T
                            elementType.classifier == Boolean::class -> values.map { it.toBoolean() } as T
                            elementType.classifier == Char::class -> values.map { it.single() } as T
                            elementType.classifier == Byte::class -> values.map { it.toByte() } as T
                            elementType.classifier == Short::class -> values.map { it.toShort() } as T
                            else -> throw IllegalArgumentException("Unsupported list element type: ${elementType.classifier}")
                        }
                    }
                }

                // Handle Objects
                isObjectType(kType) -> {
                    deserializeObject(allQueryParams, kType) as T
                }

                // Handle Wirespec.Enum
                isWirespecEnum(kType) -> {
                    val value = allQueryParams[name]?.firstOrNull()
                        ?: throw IllegalArgumentException("Missing enum value for $name")
                    val enumClass = kType.classifier as KClass<*>
                    enumClass.java.enumConstants.firstOrNull {
                        (it as Wirespec.Enum).label == value
                    }?.let { it as T }
                        ?: throw IllegalArgumentException("Invalid enum value: $value")
                }

                // Handle primitives
                else -> {
                    val value = allQueryParams[name]?.firstOrNull()
                        ?: throw IllegalArgumentException("Missing value for $name")
                    when (classifier) {
                        String::class -> value as T
                        Int::class -> value.toInt() as T
                        Long::class -> value.toLong() as T
                        Double::class -> value.toDouble() as T
                        Float::class -> value.toFloat() as T
                        Boolean::class -> value.toBoolean() as T
                        Char::class -> value.single() as T
                        Byte::class -> value.toByte() as T
                        Short::class -> value.toShort() as T
                        else -> throw IllegalArgumentException("Unsupported type: $classifier")
                    }
                }
            }
        }
    }

    private fun isObjectType(type: KType): Boolean {
        val classifier = type.classifier as? KClass<*> ?: return false

        // Consider primitive types and their nullable variants
        return when (classifier) {
            String::class,
            Int::class,
            Long::class,
            Double::class,
            Float::class,
            Boolean::class,
            Char::class,
            Byte::class,
            Short::class -> false
            else -> !classifier.supertypes.any { it.classifier == Wirespec.Enum::class }
        }
    }

    fun isWirespecEnum(type: KType): Boolean {
        val classifier = type.classifier as? KClass<*> ?: return false
        return classifier.supertypes.any { it.classifier == Wirespec.Enum::class}
    }

    private fun <T> serializeObject(value: T): Map<String, List<String>> {
        val kClass = value!!::class
        return kClass.memberProperties
            .filter { it.visibility == KVisibility.PUBLIC }
            .associate { prop ->
                val propValue = prop.getter.call(value)
                if (propValue != null) {
                    prop.name to listOf(propValue.toString())  // No parent name prefix
                } else {
                    prop.name to emptyList()  // No parent name prefix
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
            prop.name to values.mapNotNull { value ->  // No parent name prefix
                value?.let { prop.getter.call(it)?.toString() }
            }
        }.filterValues { it.isNotEmpty() }
    }

    private fun <T> deserializeObject(queryParams: Map<String, List<String>>, kType: KType): T {
        val kClass = kType.classifier as KClass<*>

        // Get the primary constructor
        val constructor = kClass.primaryConstructor
            ?: throw IllegalArgumentException("Class ${kClass.simpleName} must have a primary constructor")

        // Map constructor parameters to their values
        val arguments = constructor.parameters.associate { param ->
            val paramType = param.type
            val paramName = param.name ?: throw IllegalArgumentException("Constructor parameter must have a name")

            val paramValue = queryParams[paramName]?.firstOrNull()

            val value = when {
                // If parameter is missing
                paramValue == null -> {
                    // For nullable parameters, we can return null
                    if (paramType.isMarkedNullable) {
                        null
                    } else {
                        // For non-nullable parameters, we must throw an error
                        throw IllegalArgumentException("Missing required non-nullable parameter: $paramName")
                    }
                }
                // If parameter is present, convert it to the right type
                else -> {
                    when {
                        // Handle Wirespec.Enum
                        isWirespecEnum(paramType) -> {
                            val enumClass = paramType.classifier as KClass<*>
                            enumClass.java.enumConstants.firstOrNull {
                                (it as Wirespec.Enum).label == paramValue
                            } ?: throw IllegalArgumentException("Invalid enum value: $paramValue")
                        }
                        // Handle primitive types
                        paramType.classifier == String::class -> paramValue
                        paramType.classifier == Int::class -> paramValue.toInt()
                        paramType.classifier == Long::class -> paramValue.toLong()
                        paramType.classifier == Double::class -> paramValue.toDouble()
                        paramType.classifier == Float::class -> paramValue.toFloat()
                        paramType.classifier == Boolean::class -> paramValue.toBoolean()
                        paramType.classifier == Char::class -> paramValue.single()
                        paramType.classifier == Byte::class -> paramValue.toByte()
                        paramType.classifier == Short::class -> paramValue.toShort()
                        else -> throw IllegalArgumentException("Unsupported parameter type: ${paramType.classifier}")
                    }
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

        // Get the maximum length of any parameter array
        val maxLength = queryParams.values.maxOfOrNull { it.size } ?: 0

        // For each index, ensure all non-nullable parameters are present
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

        return (0 until maxLength).map { index ->
            val arguments = constructor.parameters.associate { param ->
                val paramName = param.name ?: throw IllegalArgumentException("Constructor parameter must have a name")
                val paramType = param.type

                val values = queryParams[paramName]
                val paramValue = values?.getOrNull(index)

                val value = when {
                    paramValue == null -> {
                        if (paramType.isMarkedNullable) {
                            null
                        } else {
                            // This should never happen due to our earlier validation
                            throw IllegalArgumentException("Missing required non-nullable parameter: $paramName at index $index")
                        }
                    }
                    else -> {
                        when (paramType.classifier) {
                            String::class -> paramValue
                            Int::class -> paramValue.toInt()
                            Long::class -> paramValue.toLong()
                            Double::class -> paramValue.toDouble()
                            Float::class -> paramValue.toFloat()
                            Boolean::class -> paramValue.toBoolean()
                            Char::class -> paramValue.single()
                            Byte::class -> paramValue.toByte()
                            Short::class -> paramValue.toShort()
                            else -> throw IllegalArgumentException("Unsupported parameter type: ${paramType.classifier}")
                        }
                    }
                }
                param to value
            }
            constructor.callBy(arguments)
        }
    }
}

