package community.flock.wirespec.integration.spring.java.configuration;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableWirespecController
@EnableWirespecWebClient
public @interface EnableWirespec {
}
