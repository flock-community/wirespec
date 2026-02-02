package community.flock.wirespec.integration.spring.java.application;

import community.flock.wirespec.integration.spring.java.generated.model.Pet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@org.springframework.stereotype.Service
public class Service {

    public final List<byte[]> files = new ArrayList<>();
    public final List<Pet> list = new ArrayList<>();

    public List<byte[]> getFiles() {
        return files;
    }

    public List<Pet> getList() {
        return list;
    }

    public Pet create(Pet pet) {
        list.add(pet);
        return pet;
    }

    public Pet read(Long id) {
        return list.stream()
            .filter(it -> it.id().isPresent() && Objects.equals(it.id().get(), id))
            .findFirst()
            .orElse(null);
    }

    public Pet update(Pet pet) {
        pet.id().ifPresent(id ->
            list.removeIf(it -> it.id().isPresent() && Objects.equals(it.id().get(), id))
        );
        list.add(pet);
        return pet;
    }

    public void delete(Long id) {
        list.removeIf(it -> it.id().isPresent() && Objects.equals(it.id().get(), id));
    }

    public void upload(byte[] file) {
        files.add(file);
    }
}
