package community.flock.wirespec.integration.spring.kotlin.application

import community.flock.wirespec.integration.spring.kotlin.generated.AddPetEndpoint
import community.flock.wirespec.integration.spring.kotlin.generated.DeletePetEndpoint
import community.flock.wirespec.integration.spring.kotlin.generated.FindPetsByTagsEndpoint
import community.flock.wirespec.integration.spring.kotlin.generated.GetPetByIdEndpoint
import community.flock.wirespec.integration.spring.kotlin.generated.UpdatePetEndpoint
import org.springframework.web.bind.annotation.RestController

@RestController
class Controller(
    private val service: Service
) : AddPetEndpoint.Handler, GetPetByIdEndpoint.Handler, UpdatePetEndpoint.Handler, DeletePetEndpoint.Handler, FindPetsByTagsEndpoint.Handler {

    override suspend fun addPet(request: AddPetEndpoint.Request): AddPetEndpoint.Response<*> {
        service.create(request.body)
        return AddPetEndpoint.Response200(request.body)
    }

    override suspend fun getPetById(request: GetPetByIdEndpoint.Request): GetPetByIdEndpoint.Response<*> {
        return service.list.find { it.id == request.path.petId }
            ?.let { GetPetByIdEndpoint.Response200(it) }
            ?: GetPetByIdEndpoint.Response404(Unit)
    }

    override suspend fun updatePet(request: UpdatePetEndpoint.Request): UpdatePetEndpoint.Response<*> {
        service.update(request.body)
        return UpdatePetEndpoint.Response200(request.body)
    }

    override suspend fun deletePet(request: DeletePetEndpoint.Request): DeletePetEndpoint.Response<*> {
        val id = 1L
        return service.delete(id).let {
            DeletePetEndpoint.Response400(Unit)
        }
    }

    override suspend fun findPetsByTags(request: FindPetsByTagsEndpoint.Request): FindPetsByTagsEndpoint.Response<*> =
        FindPetsByTagsEndpoint.Response200(emptyList())

}
