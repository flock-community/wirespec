package community.flock.wirespec.examples.open_api_app

import community.flock.wirespec.generated.ListPets
import community.flock.wirespec.generated.Pet
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/pets")
class TodoController(
    private val petstoreClient: PetstoreClient
) {

    @GetMapping("/")
    suspend fun list(): List<Pet> {
        return when (val res = petstoreClient.listPetsUnit(10)) {
            is ListPets.Response200ApplicationJson -> res.content.body
            is ListPets.Response500ApplicationJson -> error("Something went wrong")
        }
    }


}
