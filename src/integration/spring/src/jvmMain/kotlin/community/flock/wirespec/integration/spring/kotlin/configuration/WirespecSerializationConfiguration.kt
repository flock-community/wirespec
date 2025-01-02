package community.flock.wirespec.integration.spring.kotlin.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.jackson.kotlin.WirespecModuleKotlin
import community.flock.wirespec.integration.spring.kotlin.web.WirespecResponseBodyAdvice
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
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
            return when {
                // TODO check wirespec enum, we need to use label value there
                isIterableOfObjects(kType) && value is Iterable<*> ->
                    mapOf(name to value.map { wirespecObjectMapper.writeValueAsString(it) })
                isComplexObject(kType) ->
                    mapOf(name to listOf(wirespecObjectMapper.writeValueAsString(value)))
                value is Iterable<*> ->
                    mapOf(name to value.map { it.toString() })
                else ->
                    mapOf(name to listOf(value.toString()))
            }
        }

        override fun <T> deserialize(raw: String, kType: KType): T =
            when {
                kType.classifier == String::class -> raw as T
                else -> wirespecObjectMapper
                    .constructType(kType.javaType)
                    .let { wirespecObjectMapper.readValue(raw, it) }
            }

        override fun <T> deserializeQuery(name: String, allQueryParams: Map<String, List<String>>, kType: KType): T {
            TODO("Not yet implemented")
        }
    }

    private fun isIterableOfObjects(kType: KType): Boolean {
        val isIterable = (kType.classifier as? KClass<*>)?.let {
            Iterable::class.isSubclassOf(it)
        } ?: false

        return if (isIterable) {
            val elementType = kType.arguments.singleOrNull()?.type?.classifier as? KClass<*>
            elementType?.let { isComplexObject(it) } ?: false
        } else false
    }

    private fun isComplexObject(kType: KType): Boolean {
        return (kType.classifier as? KClass<*>)?.let { isComplexObject(it) } ?: false
    }

    private fun isComplexObject(kClass: KClass<*>): Boolean {
        return when {
            kClass == String::class -> false
            kClass == Int::class -> false
            kClass == Long::class -> false
            kClass == Float::class -> false
            kClass == Double::class -> false
            kClass == Boolean::class -> false
            kClass == Char::class -> false

//            kClass.isEnum -> false
            else -> true
        }
    }
}

