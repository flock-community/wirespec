package community.flock.wirespec.integration.spring.kotlin.it.client

import community.flock.wirespec.integration.spring.kotlin.client.WirespecWebClient
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.AddPet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DeletePet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.FindPetsByTags
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetPetById
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UpdatePet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UploadFile
import org.springframework.stereotype.Component

@Component
class WirespecPetstoreWebClient(
    private val wirespecWebClient: WirespecWebClient,
) : AddPet.Handler,
    UpdatePet.Handler,
    GetPetById.Handler,
    DeletePet.Handler,
    FindPetsByTags.Handler,
    UploadFile.Handler {
    override suspend fun getPetById(request: GetPetById.Request): GetPetById.Response<*> = wirespecWebClient.send(request)

    override suspend fun addPet(request: AddPet.Request): AddPet.Response<*> = wirespecWebClient.send(request)

    override suspend fun updatePet(request: UpdatePet.Request): UpdatePet.Response<*> = wirespecWebClient.send(request)

    override suspend fun deletePet(request: DeletePet.Request): DeletePet.Response<*> = wirespecWebClient.send(request)

    override suspend fun findPetsByTags(request: FindPetsByTags.Request): FindPetsByTags.Response<*> = wirespecWebClient.send(request)

    override suspend fun uploadFile(request: UploadFile.Request): UploadFile.Response<*> = wirespecWebClient.send(request)
}
