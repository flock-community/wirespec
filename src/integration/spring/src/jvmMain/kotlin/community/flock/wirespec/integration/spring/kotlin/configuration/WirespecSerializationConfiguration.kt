package community.flock.wirespec.integration.spring.kotlin.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.jackson.kotlin.WirespecModuleKotlin
import community.flock.wirespec.integration.spring.kotlin.web.WirespecResponseBodyAdvice
import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.Wirespec.ParamSerialization
import community.flock.wirespec.kotlin.serde.DefaultParamSerialization
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import kotlin.reflect.KType
import kotlin.reflect.javaType

@Configuration
@OptIn(ExperimentalStdlibApi::class)
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
open class WirespecSerializationConfiguration {

    @Bean
    open fun queryParamSerde(): ParamSerialization = DefaultParamSerialization()

    @Bean
    open fun wirespecSerialization(
        objectMapper: ObjectMapper,
        queryParamSerde: ParamSerialization,
    ): Wirespec.Serialization<String> = object : Wirespec.Serialization<String>, ParamSerialization by queryParamSerde {

        private val wirespecObjectMapper = objectMapper.copy().registerModule(WirespecModuleKotlin())

        override fun <T> serialize(t: T, kType: KType): String = when (t) {
            is String -> t
            else -> wirespecObjectMapper.writeValueAsString(t)
        }

        override fun <T> deserialize(raw: String, kType: KType): T = when {
            kType.classifier == String::class -> raw as T
            else ->
                wirespecObjectMapper
                    .constructType(kType.javaType)
                    .let { wirespecObjectMapper.readValue(raw, it) }
        }
    }
}
