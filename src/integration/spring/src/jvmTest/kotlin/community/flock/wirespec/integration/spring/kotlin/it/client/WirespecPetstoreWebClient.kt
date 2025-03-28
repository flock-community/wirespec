package community.flock.wirespec.integration.spring.kotlin.it.client

import community.flock.wirespec.integration.spring.kotlin.client.WirespecWebClient
import community.flock.wirespec.integration.spring.kotlin.generated.AddPetEndpoint
import community.flock.wirespec.integration.spring.kotlin.generated.DeletePetEndpoint
import community.flock.wirespec.integration.spring.kotlin.generated.FindPetsByTagsEndpoint
import community.flock.wirespec.integration.spring.kotlin.generated.GetPetByIdEndpoint
import community.flock.wirespec.integration.spring.kotlin.generated.UpdatePetEndpoint
import org.springframework.stereotype.Component

@Component
class WirespecPetstoreWebClient(
    private val wirespecWebClient: WirespecWebClient,
) : AddPetEndpoint.Handler,
    UpdatePetEndpoint.Handler,
    GetPetByIdEndpoint.Handler,
    DeletePetEndpoint.Handler,
    FindPetsByTagsEndpoint.Handler {
    override suspend fun getPetById(request: GetPetByIdEndpoint.Request): GetPetByIdEndpoint.Response<*> = wirespecWebClient.send(request)

    override suspend fun addPet(request: AddPetEndpoint.Request): AddPetEndpoint.Response<*> = wirespecWebClient.send(request)

    override suspend fun updatePet(request: UpdatePetEndpoint.Request): UpdatePetEndpoint.Response<*> = wirespecWebClient.send(request)

    override suspend fun deletePet(request: DeletePetEndpoint.Request): DeletePetEndpoint.Response<*> = wirespecWebClient.send(request)

    override suspend fun findPetsByTags(request: FindPetsByTagsEndpoint.Request): FindPetsByTagsEndpoint.Response<*> = wirespecWebClient.send(request)
}
