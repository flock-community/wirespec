package community.flock.wirespec.examples.maven.spring.integration;

import community.flock.wirespec.integration.spring.java.configuration.EnableWirespec;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableWirespec
public class TodoApplication {
    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }
}
