package community.flock.wirespec.examples.spring_boot_integration;

import community.flock.wirespec.integration.spring.java.configuration.WirespecConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(WirespecConfiguration.class)
public class TodoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }
}
