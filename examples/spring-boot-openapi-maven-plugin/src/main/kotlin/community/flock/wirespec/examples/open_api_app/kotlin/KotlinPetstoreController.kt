package community.flock.wirespec.examples.open_api_app.kotlin

import community.flock.wirespec.generated.kotlin.CreatePets
import community.flock.wirespec.generated.kotlin.ListPets
import community.flock.wirespec.generated.kotlin.Pet
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/kotlin/pets")
class KotlinPetstoreController(
    private val kotlinPetstoreClient: KotlinPetstoreClient
) {

    @GetMapping
    suspend fun list(): List<Int> {
        val request = ListPets.ListPetsRequestUnit(10, null)
        return when (val res = kotlinPetstoreClient.listPets(request)) {
            is ListPets.ListPetsResponse200ApplicationJson -> res.content.body.map { it.id }
            is ListPets.ListPetsResponseDefaultApplicationJson -> error("Something went wrong")
            else -> error("No response")
        }
    }

    @PostMapping
    suspend fun create(@RequestBody pet: Pet): ResponseEntity<Unit> {
        val request = CreatePets.CreatePetsRequestApplicationJson(pet)
        return when (kotlinPetstoreClient.createPets(request)) {
            is CreatePets.CreatePetsResponse201 -> ResponseEntity.noContent().build()
            is CreatePets.CreatePetsResponse500ApplicationJson -> error("Something went wrong")
            else -> error("No response")
        }
    }

}
