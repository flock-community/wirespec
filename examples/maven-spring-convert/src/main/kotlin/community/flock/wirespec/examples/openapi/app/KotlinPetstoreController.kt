package community.flock.wirespec.examples.openapi.app

import community.flock.wirespec.generated.kotlin.v3.AddPetEndpoint
import community.flock.wirespec.generated.kotlin.v3.FindPetsByStatusEndpoint
import community.flock.wirespec.generated.kotlin.v3.FindPetsByStatusParameterStatus
import community.flock.wirespec.generated.kotlin.v3.Pet
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/kotlin/pets")
class KotlinPetstoreController(
    private val kotlinPetstoreClient: KotlinPetstoreClient
) {

    @PostMapping
    suspend fun create(): Long {
        val pet = Pet(name = "Pet", photoUrls = emptyList())
        val req = AddPetEndpoint.RequestApplicationJson(pet)
        return when (val res = kotlinPetstoreClient.addPet(req)) {
            is AddPetEndpoint.Response200ApplicationJson -> res.content.body.id ?: error("not created")
            is AddPetEndpoint.Response200ApplicationXml -> res.content.body.id ?: error("not created")
            is AddPetEndpoint.Response405Unit -> error("Something went wrong")
        }
    }

    @GetMapping
    suspend fun find(@RequestBody pet: Pet): List<Long> {
        val req = FindPetsByStatusEndpoint.RequestUnit(status = FindPetsByStatusParameterStatus.available)
        return when (val res = kotlinPetstoreClient.findPetsByStatus(req)) {
            is FindPetsByStatusEndpoint.Response200ApplicationJson -> res.content.body.mapNotNull { it.id }
            else -> error("No response")
        }
    }
}
