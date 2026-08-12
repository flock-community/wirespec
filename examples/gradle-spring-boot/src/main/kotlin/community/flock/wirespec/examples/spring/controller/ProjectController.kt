package community.flock.wirespec.examples.spring.controller

import community.flock.wirespec.examples.spring.generated.endpoint.CreateProject
import community.flock.wirespec.examples.spring.generated.endpoint.DeleteProject
import community.flock.wirespec.examples.spring.generated.endpoint.GetProject
import community.flock.wirespec.examples.spring.generated.endpoint.GetProjects
import community.flock.wirespec.examples.spring.generated.endpoint.UpdateProject
import community.flock.wirespec.examples.spring.generated.model.Error
import community.flock.wirespec.examples.spring.service.ProjectService
import org.springframework.web.bind.annotation.RestController

@RestController
class ProjectController(private val service: ProjectService) :
    GetProjects.Handler,
    GetProject.Handler,
    CreateProject.Handler,
    UpdateProject.Handler,
    DeleteProject.Handler {

    override suspend fun getProjects(request: GetProjects.Request): GetProjects.Response<*> =
        GetProjects.Response200(service.list())

    override suspend fun getProject(request: GetProject.Request): GetProject.Response<*> =
        service.get(request.path.id)
            ?.let { GetProject.Response200(it) }
            ?: GetProject.Response404(Error(404, "Project ${request.path.id.value} not found"))

    override suspend fun createProject(request: CreateProject.Request): CreateProject.Response<*> =
        CreateProject.Response201(service.create(request.body))

    override suspend fun updateProject(request: UpdateProject.Request): UpdateProject.Response<*> =
        service.update(request.path.id, request.body)
            ?.let { UpdateProject.Response200(it) }
            ?: UpdateProject.Response404(Error(404, "Project ${request.path.id.value} not found"))

    override suspend fun deleteProject(request: DeleteProject.Request): DeleteProject.Response<*> =
        if (service.delete(request.path.id)) DeleteProject.Response204
        else DeleteProject.Response404(Error(404, "Project ${request.path.id.value} not found"))
}
