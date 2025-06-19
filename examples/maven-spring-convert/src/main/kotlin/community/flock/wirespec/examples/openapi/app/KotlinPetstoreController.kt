package community.flock.wirespec.examples.openapi.app

import community.flock.wirespec.generated.kotlin.v3.endpoint.AddPet
import community.flock.wirespec.generated.kotlin.v3.endpoint.FindPetsByStatus
import community.flock.wirespec.generated.kotlin.v3.model.FindPetsByStatusParameterStatus
import community.flock.wirespec.generated.kotlin.v3.model.Pet
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
        val pet = Pet(name = "Pet", photoUrls = emptyList(), id = null, category = null, tags = null, status = null)
        val req = AddPet.Request(pet)
        return when (val res = kotlinPetstoreClient.addPet(req)) {
            is AddPet.Response200 -> res.body.id ?: error("not created")
            is AddPet.Response405 -> error("Something went wrong")
            else -> error("Something went wrong")
        }
    }

    @GetMapping
    suspend fun find(@RequestBody pet: Pet): List<Long> {
        val req = FindPetsByStatus.Request(status = FindPetsByStatusParameterStatus.available)
        return when (val res = kotlinPetstoreClient.findPetsByStatus(req)) {
            is FindPetsByStatus.Response200 -> res.body.mapNotNull { it.id }
            else -> error("No response")
        }
    }
}
