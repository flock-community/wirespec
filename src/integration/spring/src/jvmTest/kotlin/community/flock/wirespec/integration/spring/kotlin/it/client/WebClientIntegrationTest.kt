package community.flock.wirespec.integration.spring.kotlin.it.client

import community.flock.wirespec.integration.spring.kotlin.configuration.EnableWirespecWebClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnableWirespecWebClient
class WebClientIntegrationTest {

    @Autowired
    lateinit var wirespecPetstoreWebClient: WirespecPetstoreWebClient

//    @Test
//    fun `CRUD WebClient integration test`(): Unit = runBlocking {
//
//        val pet = Pet(
//            id = 1,
//            name = "Dog",
//            photoUrls = listOf(),
//            category = null,
//            tags = null,
//            status = null,
//        )
//
//        val addPetResponse = wirespecPetstoreWebClient.addPet(AddPetEndpoint.Request(pet))
//        assertEquals(AddPetEndpoint.Response200(pet), addPetResponse)
//
//        val updatedPet = pet.copy(name = "Cat")
//        val updatePetResponse = wirespecPetstoreWebClient.updatePet(UpdatePetEndpoint.Request(updatedPet))
//        assertEquals(UpdatePetEndpoint.Response200(updatedPet), updatePetResponse)
//
//        val getPetResponse = wirespecPetstoreWebClient.getPetById(GetPetByIdEndpoint.Request(pet.id!!))
//        assertEquals(GetPetByIdEndpoint.Response200(updatedPet), getPetResponse)
//
//        val deletePetResponse = wirespecPetstoreWebClient.deletePet(DeletePetEndpoint.Request(pet.id, null))
//        assertEquals(DeletePetEndpoint.Response400(Unit), deletePetResponse)
//    }
//
//    @Test
//    fun `query parameters`() = runBlocking {
//        val queryParamResponse = wirespecPetstoreWebClient.findPetsByTags(FindPetsByTagsEndpoint.Request(listOf("Smilodon", "Dodo", "Mammoth")))
//        assertEquals(FindPetsByTagsEndpoint.Response200(emptyList()), queryParamResponse)
//    }
}
