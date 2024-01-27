package community.flock.wirespec.integration.spring.annotations

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.Wirespec
import community.flock.wirespec.integration.spring.JacksonContentMapper
import community.flock.wirespec.integration.spring.annotations.ExtensionFunctions.getStaticField
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.util.pattern.PathPatternParser

@Configuration
@Import(WirespecResponseBodyAdvice::class, WirespecWebMvcConfiguration::class)
open class WirespecConfiguration {


    @Bean
    open fun contentMapper(objectMapper: ObjectMapper) = JacksonContentMapper(objectMapper)

    @Bean
    open fun registerWirespecController(
        contentMapper: JacksonContentMapper,
        applicationContext: ApplicationContext,
        requestMappingHandlerMapping: RequestMappingHandlerMapping
    ): List<String> {
        val options = RequestMappingInfo.BuilderConfiguration()
            .apply {
                patternParser = PathPatternParser()
            }

        return applicationContext.getBeansWithAnnotation(WirespecController::class.java)
            .flatMap { controller ->
                controller.value.javaClass.interfaces.toList()
                    .filter { Wirespec.Endpoint::class.java.isAssignableFrom(it) }
                    .map { endpoint ->
                        val path = endpoint.getStaticField("PATH")?.get(endpoint) as String
                        val method = endpoint.getStaticField("METHOD")?.get(endpoint) as String
                        val requestMappingWirespec = RequestMappingInfo
                            .paths(path)
                            .methods(RequestMethod.valueOf(method))
                            .options(options)
                            .build();
                        val ignoredMethods = listOf("REQUEST_MAPPER", "RESPONSE_MAPPER")
                        val func = endpoint.methods.first { !ignoredMethods.contains(it.name) }
                        requestMappingHandlerMapping.registerMapping(requestMappingWirespec, controller.value, func)
                        path
                    }
            }
    }


}
