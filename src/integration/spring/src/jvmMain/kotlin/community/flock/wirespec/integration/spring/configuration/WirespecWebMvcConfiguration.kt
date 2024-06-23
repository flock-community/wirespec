package community.flock.wirespec.integration.spring.configuration

import community.flock.wirespec.Wirespec
import community.flock.wirespec.integration.spring.web.WirespecMethodArgumentResolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.io.BufferedReader


@Configuration
open class WirespecWebMvcConfiguration : WebMvcConfigurer {

    @Autowired
    lateinit var contentMapper: Wirespec.ContentMapper<BufferedReader>

    override fun addArgumentResolvers(argumentResolvers: MutableList<HandlerMethodArgumentResolver>) {
        argumentResolvers.add(WirespecMethodArgumentResolver(contentMapper))
    }

}