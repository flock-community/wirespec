package community.flock.wirespec.examples.spring.controller

import community.flock.wirespec.examples.spring.generated.endpoint.CreateTaskForProject
import community.flock.wirespec.examples.spring.generated.endpoint.DeleteTask
import community.flock.wirespec.examples.spring.generated.endpoint.GetTask
import community.flock.wirespec.examples.spring.generated.endpoint.GetTasksForProject
import community.flock.wirespec.examples.spring.generated.endpoint.UpdateTask
import community.flock.wirespec.examples.spring.generated.model.Error
import community.flock.wirespec.examples.spring.service.TaskService
import org.springframework.web.bind.annotation.RestController

@RestController
class TaskController(private val service: TaskService) :
    GetTasksForProject.Handler,
    CreateTaskForProject.Handler,
    GetTask.Handler,
    UpdateTask.Handler,
    DeleteTask.Handler {

    override suspend fun getTasksForProject(request: GetTasksForProject.Request): GetTasksForProject.Response<*> =
        service.list(request.path.projectId, request.queries.status)
            ?.let { GetTasksForProject.Response200(it) }
            ?: GetTasksForProject.Response404(Error(404, "Project ${request.path.projectId.value} not found"))

    override suspend fun createTaskForProject(request: CreateTaskForProject.Request): CreateTaskForProject.Response<*> =
        service.create(request.path.projectId, request.body)
            ?.let { CreateTaskForProject.Response201(it) }
            ?: CreateTaskForProject.Response404(Error(404, "Project ${request.path.projectId.value} not found"))

    override suspend fun getTask(request: GetTask.Request): GetTask.Response<*> =
        service.get(request.path.id)
            ?.let { GetTask.Response200(it) }
            ?: GetTask.Response404(Error(404, "Task ${request.path.id.value} not found"))

    override suspend fun updateTask(request: UpdateTask.Request): UpdateTask.Response<*> =
        service.update(request.path.id, request.body)
            ?.let { UpdateTask.Response200(it) }
            ?: UpdateTask.Response404(Error(404, "Task ${request.path.id.value} not found"))

    override suspend fun deleteTask(request: DeleteTask.Request): DeleteTask.Response<*> =
        if (service.delete(request.path.id)) DeleteTask.Response204
        else DeleteTask.Response404(Error(404, "Task ${request.path.id.value} not found"))
}
