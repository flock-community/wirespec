package community.flock.wirespec.examples.spring.controller

import community.flock.wirespec.examples.spring.generated.endpoint.CreateMember
import community.flock.wirespec.examples.spring.generated.endpoint.GetMember
import community.flock.wirespec.examples.spring.generated.endpoint.GetMembers
import community.flock.wirespec.examples.spring.generated.model.Error
import community.flock.wirespec.examples.spring.service.MemberService
import org.springframework.web.bind.annotation.RestController

@RestController
class MemberController(private val service: MemberService) :
    GetMembers.Handler,
    GetMember.Handler,
    CreateMember.Handler {

    override suspend fun getMembers(request: GetMembers.Request): GetMembers.Response<*> =
        GetMembers.Response200(service.list())

    override suspend fun getMember(request: GetMember.Request): GetMember.Response<*> =
        service.get(request.path.id)
            ?.let { GetMember.Response200(it) }
            ?: GetMember.Response404(Error(404, "Member ${request.path.id.value} not found"))

    override suspend fun createMember(request: CreateMember.Request): CreateMember.Response<*> =
        CreateMember.Response201(service.create(request.body))
}
