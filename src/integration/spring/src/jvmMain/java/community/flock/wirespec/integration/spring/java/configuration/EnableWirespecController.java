package community.flock.wirespec.integration.spring.java.configuration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(WirespecSerializationConfiguration.class)
public @interface EnableWirespecController {
}
