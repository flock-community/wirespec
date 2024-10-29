package community.flock.wirespec.integration.spring.java.configuration

import community.flock.wirespec.integration.spring.java.web.WirespecMethodArgumentResolver
import community.flock.wirespec.java.Wirespec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class WirespecWebMvcConfiguration : WebMvcConfigurer {

    @Autowired
    lateinit var wirespecSerialization: Wirespec.Serialization<String>

    override fun addArgumentResolvers(argumentResolvers: MutableList<HandlerMethodArgumentResolver>) {
        argumentResolvers.add(WirespecMethodArgumentResolver(wirespecSerialization))
    }

}
