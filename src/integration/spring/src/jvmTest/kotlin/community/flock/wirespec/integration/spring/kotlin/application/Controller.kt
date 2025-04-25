package community.flock.wirespec.integration.spring.kotlin.application

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.AddPet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DeletePet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.FindPetsByTags
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetPetById
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UpdatePet
import org.springframework.web.bind.annotation.RestController

@RestController
class Controller(
    private val service: Service,
) : AddPet.Handler,
    GetPetById.Handler,
    UpdatePet.Handler,
    DeletePet.Handler,
    FindPetsByTags.Handler {

    override suspend fun addPet(request: AddPet.Request): AddPet.Response<*> {
        service.create(request.body)
        return AddPet.Response200(request.body)
    }

    override suspend fun getPetById(request: GetPetById.Request): GetPetById.Response<*> = service.list.find { it.id == request.path.petId }
        ?.let { GetPetById.Response200(it) }
        ?: GetPetById.Response404(Unit)

    override suspend fun updatePet(request: UpdatePet.Request): UpdatePet.Response<*> {
        service.update(request.body)
        return UpdatePet.Response200(request.body)
    }

    override suspend fun deletePet(request: DeletePet.Request): DeletePet.Response<*> {
        val id = 1L
        return service.delete(id).let {
            DeletePet.Response400(Unit)
        }
    }

    override suspend fun findPetsByTags(request: FindPetsByTags.Request): FindPetsByTags.Response<*> = FindPetsByTags.Response200(emptyList())
}
