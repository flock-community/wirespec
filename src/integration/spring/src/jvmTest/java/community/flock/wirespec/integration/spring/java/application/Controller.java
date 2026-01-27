package community.flock.wirespec.integration.spring.java.application;

import community.flock.wirespec.integration.spring.java.generated.endpoint.*;
import community.flock.wirespec.integration.spring.java.generated.model.ApiResponse;
import community.flock.wirespec.integration.spring.java.generated.model.Pet;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
public class Controller implements 
    AddPet.Handler,
    GetPetById.Handler,
    UpdatePet.Handler,
    DeletePet.Handler,
    FindPetsByTags.Handler,
    UploadFile.Handler {

    private final Service service;

    public Controller(Service service) {
        this.service = service;
    }

    @Override
    public CompletableFuture<AddPet.Response<?>> addPet(AddPet.Request request) {
        service.create(request.body());
        return CompletableFuture.completedFuture(new AddPet.Response200(Optional.of(100), request.body()));
    }

    @Override
    public CompletableFuture<GetPetById.Response<?>> getPetById(GetPetById.Request request) {
        Optional<Pet> pet = service.list.stream()
                .filter(it -> it.id().isPresent() && it.id().get().equals(request.path().petId()))
                .findFirst();

        GetPetById.Response<?> response = pet
                .<GetPetById.Response<?>>map(GetPetById.Response200::new)
                .orElseGet(GetPetById.Response404::new);

        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<UpdatePet.Response<?>> updatePet(UpdatePet.Request request) {
        service.update(request.body());
        return CompletableFuture.completedFuture(new UpdatePet.Response200(request.body()));
    }

    @Override
    public CompletableFuture<DeletePet.Response<?>> deletePet(DeletePet.Request request) {
        long id = 1L;
        service.delete(id);
        return CompletableFuture.completedFuture(new DeletePet.Response400());
    }

    @Override
    public CompletableFuture<FindPetsByTags.Response<?>> findPetsByTags(FindPetsByTags.Request request) {
        return CompletableFuture.completedFuture(new FindPetsByTags.Response200(Collections.emptyList()));
    }

    @Override
    public CompletableFuture<UploadFile.Response<?>> uploadFile(UploadFile.Request request) {
        Optional<byte[]> file = request.body().file();
        if (file.isEmpty()) throw new RuntimeException("Missing file");
        service.upload(file.get());
        return CompletableFuture.completedFuture(
            new UploadFile.Response200(
                new ApiResponse(
                    Optional.of(200),
                    Optional.of("type"),
                    Optional.of(request.body().additionalMetadata().orElse("none"))
                )
            )
        );
    }
}
