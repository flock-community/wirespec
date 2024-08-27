package community.flock.wirespec.examples.open_api_app

import community.flock.wirespec.generated.kotlin.v3.AddPet
import community.flock.wirespec.generated.kotlin.v3.FindPetsByStatus
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
        val pet = Pet(id = null, name = "Pet", category = null, photoUrls = emptyList(), tags = null, status = null)
        val req = AddPet.Endpoint.RequestApplicationJson(pet)
        return when (val res = kotlinPetstoreClient.addPet(req)) {
            is AddPet.Endpoint.Response200ApplicationJson -> res.body.id ?: error("not created")
            is AddPet.Endpoint.Response200ApplicationXml -> res.body.id ?: error("not created")
            is AddPet.Endpoint.Response405Unit -> error("Something went wrong")
        }
    }

    @GetMapping
    suspend fun find(@RequestBody pet: Pet): List<Long> {
        val req = FindPetsByStatus.Endpoint.RequestUnit(status = FindPetsByStatusParameterStatus.available, Unit)
        return when (val res = kotlinPetstoreClient.findPetsByStatus(req)) {
            is FindPetsByStatus.Endpoint.Response200ApplicationJson -> res.body.mapNotNull { it.id }
            else -> error("No response")
        }
    }

}
