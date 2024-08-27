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
        object : Wirespec.Mapper<ByteArray> {
            override fun <T : Any> read(raw: ByteArray, kType: KType): T = with(objectMapper) {
                readValue(raw, constructType(kType.javaType))
            }

            override fun <T : Any> write(t: T, kType: KType) = objectMapper.writeValueAsBytes(t)
        }
}
