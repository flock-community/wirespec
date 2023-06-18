package community.flock.wirespec.examples.open_api_app.java

import community.flock.wirespec.generated.java.CreatePets
import community.flock.wirespec.generated.java.ListPets
import community.flock.wirespec.generated.java.Pet
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/java/pets")
class JavaPetstoreController(
    private val javaPetstoreClient: JavaPetstoreClient
) {

    @GetMapping
    suspend fun list(): List<Int> {
        val request = ListPets.ListPetsRequestVoid(10, null)
        return when (val res = javaPetstoreClient.listPets(request)) {
            is ListPets.ListPetsResponse200ApplicationJson -> res.content.body.map { it.id }
            is ListPets.ListPetsResponseDefaultApplicationJson -> error("Something went wrong")
            else -> error("No response")
        }
    }

    @PostMapping
    suspend fun create(@RequestBody pet: Pet): ResponseEntity<Unit> {
        val request = CreatePets.CreatePetsRequestApplicationJson(pet)
        return when (javaPetstoreClient.createPets(request)) {
            is CreatePets.CreatePetsResponse201 -> ResponseEntity.noContent().build()
            is CreatePets.CreatePetsResponse500ApplicationJson -> error("Something went wrong")
            else -> error("No response")
        }
    }

}
