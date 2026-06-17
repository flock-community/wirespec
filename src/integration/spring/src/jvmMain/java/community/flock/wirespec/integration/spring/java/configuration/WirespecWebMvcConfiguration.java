package community.flock.wirespec.integration.spring.java.configuration;

import community.flock.wirespec.integration.spring.java.web.WirespecMethodArgumentResolver;
import community.flock.wirespec.integration.spring.shared.WirespecJsonMapper;
import community.flock.wirespec.java.Wirespec;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WirespecWebMvcConfiguration implements WebMvcConfigurer {

    private final Wirespec.Serialization wirespecSerialization;
    private final WirespecJsonMapper wirespecJsonMapper;

    public WirespecWebMvcConfiguration(Wirespec.Serialization wirespecSerialization, WirespecJsonMapper wirespecJsonMapper) {
        this.wirespecSerialization = wirespecSerialization;
        this.wirespecJsonMapper = wirespecJsonMapper;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new WirespecMethodArgumentResolver(wirespecSerialization, wirespecJsonMapper));
    }
}
