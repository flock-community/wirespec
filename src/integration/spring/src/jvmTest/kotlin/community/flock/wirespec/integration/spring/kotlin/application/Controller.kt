package community.flock.wirespec.integration.spring.kotlin.application

//import community.flock.wirespec.integration.spring.generated.AddPetEndpoint
//import community.flock.wirespec.integration.spring.generated.DeletePetEndpoint
//import community.flock.wirespec.integration.spring.generated.GetPetByIdEndpoint
//import community.flock.wirespec.integration.spring.generated.UpdatePetEndpoint
//import org.springframework.web.bind.annotation.RestController
//
//@RestController
//class Controller(
//    private val service: Service
//) : AddPetEndpoint, GetPetByIdEndpoint, UpdatePetEndpoint, DeletePetEndpoint {
//
//    override suspend fun addPet(request: AddPetEndpoint.Request<*>): AddPetEndpoint.Response<*> {
//        val createdPet = when (request) {
//            else -> TODO()
//        }
//        return AddPetEndpoint.Response200ApplicationJson(createdPet)
//    }
//
//    override suspend fun getPetById(request: GetPetByIdEndpoint.Request<*>): GetPetByIdEndpoint.Response<*> {
//        val id = ""
//        return service.list.find { it.id == id?.toLong() }
//            ?.let { GetPetByIdEndpoint.Response200ApplicationJson(it) }
//            ?: GetPetByIdEndpoint.Response400Unit()
//    }
//
//    override suspend fun updatePet(request: UpdatePetEndpoint.Request<*>): UpdatePetEndpoint.Response<*> {
//        val updatedPet = when (request) {
//            else -> error("Cannot handle request")
//        }
//        return UpdatePetEndpoint.Response200ApplicationJson(updatedPet)
//    }
//
//    override suspend fun deletePet(request: DeletePetEndpoint.Request<*>): DeletePetEndpoint.Response<*> {
//        val id = 1L
//        return service.delete(id).let {
//            DeletePetEndpoint.Response400Unit()
//        }
//    }
//
//}