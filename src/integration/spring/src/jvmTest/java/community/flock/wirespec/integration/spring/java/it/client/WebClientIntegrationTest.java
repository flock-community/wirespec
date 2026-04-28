package community.flock.wirespec.integration.spring.java.it.client;

import community.flock.wirespec.integration.spring.java.application.Application;
import community.flock.wirespec.integration.spring.java.generated.endpoint.*;
import community.flock.wirespec.integration.spring.java.generated.model.ApiResponse;
import community.flock.wirespec.integration.spring.java.generated.model.Pet;
import community.flock.wirespec.integration.spring.java.generated.model.UploadFileRequestBody;
import community.flock.wirespec.integration.spring.java.generated.model.UploadFileRequestBodyJson;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = {Application.class, WirespecPetstoreWebClient.class})
@EnableConfigurationProperties
public class WebClientIntegrationTest {

    @Autowired
    private WirespecPetstoreWebClient wirespecPetstoreWebClient;

    @Test
    public void crudWebClientIntegrationTest() throws ExecutionException, InterruptedException {
        Pet pet = new Pet(
                Optional.of(1L),
                "Dog",
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty()
        );

        AddPet.Response<?> addPetResponse = wirespecPetstoreWebClient.addPet(new AddPet.Request(pet)).get();
        assertEquals(new AddPet.Response200(Optional.of(100), pet), addPetResponse);

        Pet updatedPet = new Pet(
                pet.id(),
                "Cat",
                pet.category(),
                pet.photoUrls(),
                pet.tags(),
                pet.status()
        );
        UpdatePet.Response<?> updatePetResponse = wirespecPetstoreWebClient.updatePet(new UpdatePet.Request(updatedPet)).get();
        assertEquals(new UpdatePet.Response200(updatedPet), updatePetResponse);

        GetPetById.Response<?> getPetResponse = wirespecPetstoreWebClient.getPetById(new GetPetById.Request(pet.id().orElseThrow())).get();
        assertEquals(new GetPetById.Response200(updatedPet), getPetResponse);

        DeletePet.Response<?> deletePetResponse = wirespecPetstoreWebClient.deletePet(new DeletePet.Request(pet.id().get(), Optional.empty())).get();
        assertEquals(new DeletePet.Response400(), deletePetResponse);
    }

    @Test
    public void queryParameters() throws ExecutionException, InterruptedException {
        FindPetsByTags.Response<?> queryParamResponse = wirespecPetstoreWebClient.findPetsByTags(
                new FindPetsByTags.Request(Optional.of(List.of("Smilodon", "Dodo", "Mammoth")))
        ).get();
        assertEquals(new FindPetsByTags.Response200(List.of()), queryParamResponse);
    }

    @Test
    public void multipartFormData() throws ExecutionException, InterruptedException {
        UploadFile.Request request = new UploadFile.Request(
                1L,
                Optional.of("metadata"),
                new UploadFileRequestBody(
                        Optional.of("metadata"),
                        Optional.of("data".getBytes()),
                        Optional.of(new UploadFileRequestBodyJson(Optional.of("bar")))
                )
        );
        UploadFile.Response<?> response = wirespecPetstoreWebClient.uploadFile(request).get();
        assertEquals(
                new UploadFile.Response200(new ApiResponse(Optional.of(200), Optional.of("type"), Optional.of("metadata"))),
                response
        );
    }
}
