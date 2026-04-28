package community.flock.wirespec.examples.maven.spring.integration;

import community.flock.wirespec.integration.spring.java.configuration.EnableWirespec;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableWirespec
public class TodoApplication {

    protected TodoApplication() {
        // Bootstrap class; instantiated only by Spring's context.
    }

    @SuppressWarnings("java:S3051")
    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }
}
