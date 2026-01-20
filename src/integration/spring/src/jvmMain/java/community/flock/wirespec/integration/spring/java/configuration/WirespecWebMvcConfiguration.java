package community.flock.wirespec.integration.spring.java.configuration;

import community.flock.wirespec.integration.spring.java.web.WirespecMethodArgumentResolver;
import community.flock.wirespec.java.Wirespec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static community.flock.wirespec.integration.spring.java.configuration.WirespecSerializationConfiguration.objectMapper;

@Configuration
public class WirespecWebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private Wirespec.Serialization wirespecSerialization;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new WirespecMethodArgumentResolver(wirespecSerialization));
    }
}
