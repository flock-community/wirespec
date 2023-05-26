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
        return when (val res = petstoreClient.listPets(10, null)) {
            is ListPets.Response200ApplicationJson -> res.content.body.map { it.id }
            is ListPets.Response500ApplicationJson -> error("Something went wrong")
        }
    }

    @PostMapping
    suspend fun create(@RequestBody pet: Pet): ResponseEntity<Unit> {
        return when (petstoreClient.createPetsApplicationJson(pet)) {
            is CreatePets.Response201 -> ResponseEntity.noContent().build()
            is CreatePets.Response500ApplicationJson -> TODO()
        }
    }

}
