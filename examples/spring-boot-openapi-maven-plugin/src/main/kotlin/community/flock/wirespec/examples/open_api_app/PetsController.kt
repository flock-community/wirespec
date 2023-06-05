package community.flock.wirespec.examples.open_api_app

import community.flock.wirespec.generated.CreatePets
import community.flock.wirespec.generated.ListPets
import community.flock.wirespec.generated.Pet
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/pets")
class PetsController(
    private val petstoreClient: PetstoreClient
) {

    @GetMapping
    suspend fun list(): List<Int> {
        val request = ListPets.ListPetsRequestUnit(10, null)
        return when (val res = petstoreClient.listPets(request)) {
            is ListPets.ListPetsResponse200ApplicationJson -> res.content.body.map { it.id }
        }
    }

    @PostMapping
    suspend fun create(@RequestBody pet: Pet): ResponseEntity<Unit> {
        val request = CreatePets.CreatePetsRequestApplicationJson(pet)
        return when (petstoreClient.createPets(request)) {
            is CreatePets.CreatePetsResponse201 -> ResponseEntity.noContent().build()
            is CreatePets.CreatePetsResponse500ApplicationJson -> error("Something went wrong")
        }
    }

}
