package community.flock.wirespec.example.maven.preprocessor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import community.flock.wirespec.generated.endpoint.addPetProcessed;

public class PerProcessTest {

    @Test
    void test() {
        var handlers = new addPetProcessed.Handler.Handlers();
        Assertions.assertEquals("/pet", handlers.getPathTemplate());
    }

}
