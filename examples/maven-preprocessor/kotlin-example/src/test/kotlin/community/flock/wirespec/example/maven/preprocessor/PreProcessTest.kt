package community.flock.wirespec.example.maven.preprocessor

import community.flock.wirespec.generated.endpoint.AddPetProcessed
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PreProcessTest {

    @Test
    fun test() {
        val handlers = AddPetProcessed.Handler.Handlers()
        Assertions.assertEquals("/pet", handlers.getPathTemplate())
    }
}
