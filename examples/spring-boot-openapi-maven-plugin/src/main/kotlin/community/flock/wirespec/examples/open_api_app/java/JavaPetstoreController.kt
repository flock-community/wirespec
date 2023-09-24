package community.flock.wirespec.examples.open_api_app.java

import community.flock.wirespec.generated.java.v3.AddPet
import community.flock.wirespec.generated.java.v3.FindPetsByStatus
import community.flock.wirespec.generated.java.v3.Pet
import community.flock.wirespec.generated.java.v3.FindPetsByStatusParameterStatus
import org.springframework.web.bind.annotation.*
import java.util.*
import kotlin.jvm.optionals.getOrNull

@RestController
@RequestMapping("/java/pets")
class JavaPetstoreController(
    private val javaPetstoreClient: JavaPetstoreClient
) {

    @GetMapping
    suspend fun addPet(): Optional<Int>? {
        val pet = Pet(Optional.empty(), "Petje", Optional.empty(), emptyList(), Optional.empty(), Optional.empty())
        val req = AddPet.RequestApplicationJson(pet)
        return when (val res = javaPetstoreClient.addPet(req)) {
            is AddPet.Response200ApplicationJson -> res.content?.body?.id
            else -> error("No response")
        }
    }

    @PostMapping
    suspend fun create(@RequestBody pet: Pet): List<Int> {
        val req = FindPetsByStatus.RequestVoid(Optional.of(FindPetsByStatusParameterStatus.available))
        return when (val res = javaPetstoreClient.findPetsByStatus(req)) {
            is FindPetsByStatus.Response200ApplicationJson -> res.content?.body?.mapNotNull { it.id.getOrNull() } ?: emptyList()
            else -> error("No response")
        }
    }

}
