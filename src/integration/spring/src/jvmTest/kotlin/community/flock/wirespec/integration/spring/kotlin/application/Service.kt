package community.flock.wirespec.integration.spring.kotlin.application

import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet
import org.springframework.stereotype.Service

@Service
class Service {

    val list = mutableListOf<Pet>()

    fun create(pet: Pet): Pet = pet
        .also { list.add(it) }

    fun read(id: Long): Pet? = list.find { it.id == id }

    fun update(pet: Pet): Pet {
        list.removeIf { it.id == pet.id }
        list.add(pet)
        return pet
    }

    fun delete(id: Long) {
        list.removeIf { it.id == id }
    }
}
