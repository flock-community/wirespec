package community.flock.wirespec.integration.spring.application

import community.flock.wirespec.integration.spring.annotations.WirespecController
import community.flock.wirespec.integration.spring.annotations.parsePathParams
import community.flock.wirespec.integration.spring.generated.AddPet
import community.flock.wirespec.integration.spring.generated.DeletePet
import community.flock.wirespec.integration.spring.generated.GetPetById
import community.flock.wirespec.integration.spring.generated.UpdatePet

@WirespecController
class Controller(
    private val service: Service
) : AddPet, GetPetById, UpdatePet, DeletePet {

    override suspend fun addPet(request: AddPet.Request<*>): AddPet.Response<*> {
        val createdPet = when (request) {
            is AddPet.RequestApplicationJson -> service.create(request.content!!.body)
            else -> TODO()
        }
        return AddPet.Response200ApplicationJson(emptyMap(), createdPet)
    }

    override suspend fun getPetById(request: GetPetById.Request<*>): GetPetById.Response<*> {
        val id = request.parsePathParams()["petId"]
        return service.list.find { it.id == id?.toInt() }
            ?.let { GetPetById.Response200ApplicationJson(emptyMap(), it) }
            ?: GetPetById.Response400Unit(emptyMap())
    }

    override suspend fun updatePet(request: UpdatePet.Request<*>): UpdatePet.Response<*> {
        val updatedPet = when (request) {
            is UpdatePet.RequestApplicationJson -> service.update(request.content!!.body)
            else -> TODO()
        }
        return UpdatePet.Response200ApplicationJson(emptyMap(), updatedPet)
    }

    override suspend fun deletePet(request: DeletePet.Request<*>): DeletePet.Response<*> {
        val id = request.parsePathParams()["petId"]?.toInt() ?: error("petId not found")
        return service.delete(id).let {
            DeletePet.Response400Unit(emptyMap())
        }
    }

}