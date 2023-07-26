package community.flock.wirespec.examples.open_api_app.kotlin

import community.flock.wirespec.generated.kotlin.v3.CreatePets
import community.flock.wirespec.generated.kotlin.v3.ListPets
import community.flock.wirespec.generated.kotlin.v3.Pet
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
        val request = ListPets.RequestUnit(10, null)
        return when (val res = kotlinPetstoreClient.listPets(request)) {
            is ListPets.Response200ApplicationJson -> res.content.body.map { it.id }
            is ListPets.ResponseDefaultApplicationJson -> error("Something went wrong")
            else -> error("No response")
        }
    }

    @PostMapping
    suspend fun create(@RequestBody pet: Pet): ResponseEntity<Unit> {
        val request = CreatePets.RequestApplicationJson(pet)
        return when (kotlinPetstoreClient.createPets(request)) {
            is CreatePets.Response201 -> ResponseEntity.noContent().build()
            is CreatePets.Response500ApplicationJson -> error("Something went wrong")
            else -> error("No response")
        }
    }

}
