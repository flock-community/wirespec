package community.flock.wirespec.examples.open_api_app.kotlin

import community.flock.wirespec.generated.kotlin.v3.AddPet
import community.flock.wirespec.generated.kotlin.v3.FindPetsByStatus
import community.flock.wirespec.generated.kotlin.v3.FindPetsByStatusParameterStatus
import community.flock.wirespec.generated.kotlin.v3.Pet
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/kotlin/pets")
class KotlinPetstoreController(
    private val kotlinPetstoreClient: KotlinPetstoreClient
) {

    @PostMapping
    suspend fun create(): Int {
        val pet = Pet(name = "Pet", photoUrls = emptyList())
        val req = AddPet.RequestApplicationJson(pet)
        return when (val res = kotlinPetstoreClient.addPet(req)) {
            is AddPet.Response200ApplicationJson -> res.content.body.id ?: error("not created")
            is AddPet.Response200ApplicationXml -> res.content.body.id ?: error("not created")
            is AddPet.Response405Unit -> error("Something went wrong")
        }
    }

    @GetMapping
    suspend fun find(@RequestBody pet: Pet): List<Int?> {
        val req = FindPetsByStatus.RequestUnit(status = FindPetsByStatusParameterStatus.available)
        return when (val res = kotlinPetstoreClient.findPetsByStatus(req)) {
            is FindPetsByStatus.Response200ApplicationJson -> res.content.body.map { it.id }
            else -> error("No response")
        }
    }

}
