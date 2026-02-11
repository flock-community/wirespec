package community.flock.wirespec.integration.spring.java.it.client;

import community.flock.wirespec.integration.spring.java.client.WirespecWebClient;
import community.flock.wirespec.integration.spring.java.generated.endpoint.AddPet;
import community.flock.wirespec.integration.spring.java.generated.endpoint.DeletePet;
import community.flock.wirespec.integration.spring.java.generated.endpoint.FindPetsByTags;
import community.flock.wirespec.integration.spring.java.generated.endpoint.GetPetById;
import community.flock.wirespec.integration.spring.java.generated.endpoint.UpdatePet;
import community.flock.wirespec.integration.spring.java.generated.endpoint.UploadFile;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class WirespecPetstoreWebClient implements
        AddPet.Handler,
        UpdatePet.Handler,
        GetPetById.Handler,
        DeletePet.Handler,
        FindPetsByTags.Handler,
        UploadFile.Handler {

    private final WirespecWebClient wirespecWebClient;

    public WirespecPetstoreWebClient(WirespecWebClient wirespecWebClient) {
        this.wirespecWebClient = wirespecWebClient;
    }

    @Override
    public CompletableFuture<GetPetById.Response<?>> getPetById(GetPetById.Request request) {
        return wirespecWebClient.send(request);
    }

    @Override
    public CompletableFuture<AddPet.Response<?>> addPet(AddPet.Request request) {
        return wirespecWebClient.send(request);
    }

    @Override
    public CompletableFuture<UpdatePet.Response<?>> updatePet(UpdatePet.Request request) {
        return wirespecWebClient.send(request);
    }

    @Override
    public CompletableFuture<DeletePet.Response<?>> deletePet(DeletePet.Request request) {
        return wirespecWebClient.send(request);
    }

    @Override
    public CompletableFuture<FindPetsByTags.Response<?>> findPetsByTags(FindPetsByTags.Request request) {
        return wirespecWebClient.send(request);
    }

    @Override
    public CompletableFuture<UploadFile.Response<?>> uploadFile(UploadFile.Request request) {
        return wirespecWebClient.send(request);
    }
}
