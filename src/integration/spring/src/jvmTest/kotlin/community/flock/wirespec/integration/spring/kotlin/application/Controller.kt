package community.flock.wirespec.integration.spring.kotlin.application

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.AddPet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DeletePet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.FindPetsByTags
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetPetById
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.RequestParrot
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UpdatePet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UploadFile
import community.flock.wirespec.integration.spring.kotlin.generated.model.ApiResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class Controller(
    private val service: Service,
) : AddPet.Handler,
    GetPetById.Handler,
    UpdatePet.Handler,
    DeletePet.Handler,
    FindPetsByTags.Handler,
    UploadFile.Handler,
    RequestParrot.Handler {

    override suspend fun addPet(request: AddPet.Request): AddPet.Response<*> {
        service.create(request.body)
        return AddPet.Response200(request.body, 200)
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

    override suspend fun uploadFile(request: UploadFile.Request): UploadFile.Response<*> {
        val file = request.body.file ?: error("Missing file")
        service.upload(file)
        return UploadFile.Response200(
            ApiResponse(
                code = 200,
                type = "type",
                message = request.body.additionalMetadata ?: "none",
            ),
        )
    }

    override suspend fun requestParrot(request: RequestParrot.Request): RequestParrot.Response<*> = RequestParrot.Response200(
        body = request.body,
        XRequestID = request.headers.XRequestID,
        RanDoMHeADer = request.headers.RanDoMHeADer,
        QueryParamParrot = request.queries.QueryParam,
        RanDoMQueRYParrot = request.queries.RanDoMQueRY,
    )
}
