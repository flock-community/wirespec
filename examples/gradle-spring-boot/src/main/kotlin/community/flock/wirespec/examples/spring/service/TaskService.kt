package community.flock.wirespec.examples.spring.service

import community.flock.wirespec.examples.spring.generated.model.ProjectId
import community.flock.wirespec.examples.spring.generated.model.Task
import community.flock.wirespec.examples.spring.generated.model.TaskId
import community.flock.wirespec.examples.spring.generated.model.TaskInput
import community.flock.wirespec.examples.spring.generated.model.TaskStatus
import community.flock.wirespec.examples.spring.repository.ProjectRepository
import community.flock.wirespec.examples.spring.repository.TaskRepository
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
) {

    suspend fun list(projectId: ProjectId, status: TaskStatus?): List<Task>? {
        if (projectRepository.findById(projectId) == null) return null
        return taskRepository.findByProject(projectId, status)
    }

    suspend fun get(id: TaskId): Task? = taskRepository.findById(id)

    suspend fun create(projectId: ProjectId, input: TaskInput): Task? {
        if (projectRepository.findById(projectId) == null) return null
        return taskRepository.save(
            Task(
                id = TaskId(Random.nextLong(0, 99_999)),
                projectId = projectId,
                title = input.title,
                description = input.description,
                status = input.status,
                assigneeId = input.assigneeId,
            ),
        )
    }

    suspend fun update(id: TaskId, input: TaskInput): Task? {
        val existing = taskRepository.findById(id) ?: return null
        return taskRepository.save(
            existing.copy(
                title = input.title,
                description = input.description,
                status = input.status,
                assigneeId = input.assigneeId,
            ),
        )
    }

    suspend fun delete(id: TaskId): Boolean = taskRepository.delete(id)
}
