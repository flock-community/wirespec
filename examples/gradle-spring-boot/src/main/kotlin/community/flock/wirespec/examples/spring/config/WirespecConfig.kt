package community.flock.wirespec.examples.spring.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class WirespecConfig {

    @Bean
    open fun wirespecObjectMapper(): ObjectMapper = jacksonObjectMapper()
        .registerModule(WirespecJacksonModule())

    @Bean
    open fun wirespecSerialization(objectMapper: ObjectMapper): Wirespec.Serialization =
        JacksonWirespecSerialization(objectMapper)

    @Bean
    open fun wirespecMvcConfigurer(serialization: Wirespec.Serialization): WebMvcConfigurer =
        object : WebMvcConfigurer {
            override fun addArgumentResolvers(
                resolvers: MutableList<org.springframework.web.method.support.HandlerMethodArgumentResolver>,
            ) {
                resolvers.add(0, WirespecRequestArgumentResolver(serialization))
            }
        }
}
