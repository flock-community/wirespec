package community.flock.wirespec.integration.spring.java.application;

import community.flock.wirespec.integration.spring.java.configuration.EnableWirespecController;
import community.flock.wirespec.integration.spring.java.configuration.EnableWirespecWebClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableWirespecController
@EnableWirespecWebClient
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
