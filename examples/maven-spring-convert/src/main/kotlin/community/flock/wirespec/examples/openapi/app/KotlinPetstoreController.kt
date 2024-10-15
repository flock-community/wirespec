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
        val pet = Pet(name = "Pet", photoUrls = emptyList(), id = null, category = null, tags = null, status = null)
        val req = AddPetEndpoint.Request(pet)
        return when (val res = kotlinPetstoreClient.addPet(req)) {
            is AddPetEndpoint.Response200 -> res.body.id ?: error("not created")
            is AddPetEndpoint.Response405 -> error("Something went wrong")
            else -> error("Something went wrong")
        }
    }

    @GetMapping
    suspend fun find(@RequestBody pet: Pet): List<Long> {
        val req = FindPetsByStatusEndpoint.Request(status = FindPetsByStatusParameterStatus.available)
        return when (val res = kotlinPetstoreClient.findPetsByStatus(req)) {
            is FindPetsByStatusEndpoint.Response200 -> res.body.mapNotNull { it.id }
            else -> error("No response")
        }
    }
}
