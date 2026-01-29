package community.flock.wirespec.example.maven.preprocessor

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import community.flock.wirespec.generated.endpoint.AddPetProcessed

class PreProcessTest {

    @Test
    fun test() {
        Assertions.assertEquals("/pet", AddPetProcessed.Adapter.pathTemplate)
        Assertions.assertEquals("POST", AddPetProcessed.Adapter.method)
    }
}