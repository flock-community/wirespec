package community.flock.wirespec.integration.spring.annotations

import community.flock.wirespec.integration.spring.JacksonContentMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
open class WirespecWebMvcConfiguration : WebMvcConfigurer {

    @Autowired
    lateinit var contentMapper: JacksonContentMapper

    override fun addArgumentResolvers(argumentResolvers: MutableList<HandlerMethodArgumentResolver>) {
        argumentResolvers.add(WirespecMethodArgumentResolver(contentMapper))
    }

}