package community.flock.wirespec.examples.kotest.controller

import community.flock.wirespec.examples.kotest.generated.endpoint.ActivateCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.CreateCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.DeleteCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.GetCampaign
import community.flock.wirespec.examples.kotest.generated.endpoint.GetCampaigns
import community.flock.wirespec.examples.kotest.generated.endpoint.UpdateCampaign
import community.flock.wirespec.examples.kotest.generated.model.Error
import community.flock.wirespec.examples.kotest.service.CampaignService
import org.springframework.web.bind.annotation.RestController

/** Implements the Wirespec-generated campaign `*.Handler` interfaces. */
@RestController
class CampaignController(
    private val service: CampaignService,
) : GetCampaigns.Handler,
    GetCampaign.Handler,
    CreateCampaign.Handler,
    UpdateCampaign.Handler,
    ActivateCampaign.Handler,
    DeleteCampaign.Handler {

    override suspend fun getCampaigns(request: GetCampaigns.Request): GetCampaigns.Response<*> = GetCampaigns.Response200(service.list(request.queries.status))

    override suspend fun getCampaign(request: GetCampaign.Request): GetCampaign.Response<*> = service.get(request.path.id)
        ?.let { GetCampaign.Response200(it) }
        ?: GetCampaign.Response404(notFound(request.path.id.value))

    override suspend fun createCampaign(request: CreateCampaign.Request): CreateCampaign.Response<*> = CreateCampaign.Response201(service.create(request.body))

    override suspend fun updateCampaign(request: UpdateCampaign.Request): UpdateCampaign.Response<*> = service.update(request.path.id, request.body)
        ?.let { UpdateCampaign.Response200(it) }
        ?: UpdateCampaign.Response404(notFound(request.path.id.value))

    override suspend fun activateCampaign(request: ActivateCampaign.Request): ActivateCampaign.Response<*> = service.activate(request.path.id)
        ?.let { ActivateCampaign.Response200(it) }
        ?: ActivateCampaign.Response404(notFound(request.path.id.value))

    override suspend fun deleteCampaign(request: DeleteCampaign.Request): DeleteCampaign.Response<*> = if (service.delete(request.path.id)) {
        DeleteCampaign.Response204
    } else {
        DeleteCampaign.Response404(notFound(request.path.id.value))
    }

    private fun notFound(id: String) = Error(code = 404, message = "Campaign not found: $id")
}
