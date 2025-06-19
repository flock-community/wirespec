package community.flock.wirespec.example.maven.preprocessor

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import community.flock.wirespec.generated.endpoint.AddPetProcessed

class PreProcessTest {

    @Test
    fun test() {
        val handlers = AddPetProcessed.Handler.Handlers()
        Assertions.assertEquals("/pet", handlers.getPathTemplate())
    }
}