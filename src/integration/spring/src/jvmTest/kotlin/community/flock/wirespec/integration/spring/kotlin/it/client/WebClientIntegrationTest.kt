package community.flock.wirespec.integration.spring.kotlin.it.client

import community.flock.wirespec.integration.spring.kotlin.application.Application
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.AddPet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DeletePet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.FindPetsByTags
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetPetById
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UpdatePet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UploadFile
import community.flock.wirespec.integration.spring.kotlin.generated.model.ApiResponse
import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet
import community.flock.wirespec.integration.spring.kotlin.generated.model.UploadFileRequestBody
import community.flock.wirespec.integration.spring.kotlin.generated.model.UploadFileRequestBodyJson
import community.flock.wirespec.openapi.json
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = [Application::class, WirespecPetstoreWebClient::class])
@EnableConfigurationProperties
class WebClientIntegrationTest {
    @Autowired
    lateinit var wirespecPetstoreWebClient: WirespecPetstoreWebClient

    @Test
    fun `CRUD WebClient integration test`(): Unit = runBlocking {
        val pet =
            Pet(
                id = 1,
                name = "Dog",
                photoUrls = listOf(),
                category = null,
                tags = null,
                status = null,
            )

        val addPetResponse = wirespecPetstoreWebClient.addPet(AddPet.Request(pet))
        assertEquals(AddPet.Response200(pet), addPetResponse)

        val updatedPet = pet.copy(name = "Cat")
        val updatePetResponse = wirespecPetstoreWebClient.updatePet(UpdatePet.Request(updatedPet))
        assertEquals(UpdatePet.Response200(updatedPet), updatePetResponse)

        val getPetResponse = wirespecPetstoreWebClient.getPetById(GetPetById.Request(pet.id!!))
        assertEquals(GetPetById.Response200(updatedPet), getPetResponse)

        val deletePetResponse = wirespecPetstoreWebClient.deletePet(DeletePet.Request(pet.id, null))
        assertEquals(DeletePet.Response400(Unit), deletePetResponse)
    }

    @Test
    fun `query parameters`() = runBlocking {
        val queryParamResponse =
            wirespecPetstoreWebClient.findPetsByTags(
                FindPetsByTags.Request(listOf("Smilodon", "Dodo", "Mammoth")),
            )
        assertEquals(FindPetsByTags.Response200(emptyList()), queryParamResponse)
    }

    @Test
    fun `multipart form data`() = runBlocking {
        val request = UploadFile.Request(
            petId = 1,
            additionalMetadata = "metadata",
            body = UploadFileRequestBody(
                additionalMetadata = "metadata",
                file = "data".toByteArray(),
                json = UploadFileRequestBodyJson(foo = "bar"),
            ),
        )
        val response = wirespecPetstoreWebClient.uploadFile(request)
        assertEquals(
            UploadFile.Response200(ApiResponse(code = 200, type = "type", message = "metadata")),
            response,
        )
    }
}
