package community.flock.wirespec.examples.spring_boot_integration;

import community.flock.wirespec.generated.*;
import community.flock.wirespec.integration.spring.annotations.WirespecController;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@WirespecController
class PetstoreController implements AddPet, GetPetById, UpdatePet, DeletePet {

    @Override
    public AddPet.Response addPet(AddPet.Request request) {
        Pet pet = new Pet(
                Optional.empty(),
                "Willem",
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty()
        );
        AddPet.Response200ApplicationJson res = new AddPet.Response200ApplicationJson(Map.of(), pet);
        return res;
    }

    @Override
    public DeletePet.Response deletePet(DeletePet.Request request) {
        return null;
    }

    @Override
    public GetPetById.Response getPetById(GetPetById.Request request) {
        return null;
    }

    @Override
    public UpdatePet.Response updatePet(UpdatePet.Request request) {
        return null;
    }
}
