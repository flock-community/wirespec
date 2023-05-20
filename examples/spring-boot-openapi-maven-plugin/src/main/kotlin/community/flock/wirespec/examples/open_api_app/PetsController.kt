package community.flock.wirespec.examples.open_api_app

import community.flock.wirespec.generated.ListPets
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/pets")
class TodoController(
    private val petstoreClient: PetstoreClient
) {

    @GetMapping("/")
    suspend fun list(): List<Int> {
        return when (val res = petstoreClient.listPetsUnit(10)) {
            is ListPets.Response200ApplicationJson -> res.content.body.map {
                it.id
            }
            is ListPets.Response500ApplicationJson -> error("Something went wrong")
        }
    }

}
