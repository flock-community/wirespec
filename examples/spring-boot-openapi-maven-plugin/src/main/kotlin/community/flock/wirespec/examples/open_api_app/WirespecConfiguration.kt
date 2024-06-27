@file:OptIn(ExperimentalStdlibApi::class)

package community.flock.wirespec.examples.open_api_app

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.Wirespec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.reflect.KType
import kotlin.reflect.javaType

@Configuration
class WirespecConfiguration {

    @Bean
    fun contentMapper(objectMapper: ObjectMapper) =
        object : Wirespec.ContentMapper<ByteArray> {
            override fun <T> read(
                content: Wirespec.Content<ByteArray>,
                valueType: KType,
            ): Wirespec.Content<T> = content.let {
                val type = objectMapper.constructType(valueType.javaType)
                val obj: T = objectMapper.readValue(content.body, type)
                Wirespec.Content(it.type, obj)
            }

            override fun <T> write(
                content: Wirespec.Content<T>,
                valueType: KType,
            ): Wirespec.Content<ByteArray> = content.let {
                val bytes = objectMapper.writeValueAsBytes(content.body)
                Wirespec.Content(it.type, bytes)
            }
        }
}