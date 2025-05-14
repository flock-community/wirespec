package community.flock.wirespec.example.maven.preprocessor;

import community.flock.wirespec.generated.endpoint.addPetProcessed;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PerProcessTest {

    @Test
    void test() {
        var handlers = new addPetProcessed.Handler.Handlers();
        Assertions.assertEquals("/pet", handlers.getPathTemplate());
    }

}
