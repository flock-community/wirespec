package community.flock.wirespec.example.maven.preprocessor;

import community.flock.wirespec.generated.endpoint.AddPetProcessed;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PerProcessTest {

    @Test
    void test() {
        var api = AddPetProcessed.api;
        Assertions.assertEquals("/pet", api.getPathTemplate());
    }
}
