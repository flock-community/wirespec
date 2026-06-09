package community.flock.wirespec.examples.spring.repository

import community.flock.wirespec.examples.spring.generated.model.ProjectId
import community.flock.wirespec.examples.spring.generated.model.Task
import community.flock.wirespec.examples.spring.generated.model.TaskId
import community.flock.wirespec.examples.spring.generated.model.TaskStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Repository

@Repository
class TaskRepository {

    private val mutex = Mutex()
    private val store = mutableMapOf<TaskId, Task>()

    suspend fun findById(id: TaskId): Task? = mutex.withLock { store[id] }

    suspend fun findByProject(projectId: ProjectId, status: TaskStatus?): List<Task> = mutex.withLock {
        store.values
            .filter { it.projectId == projectId }
            .filter { status == null || it.status == status }
    }

    suspend fun save(task: Task): Task = mutex.withLock {
        store[task.id] = task
        task
    }

    suspend fun delete(id: TaskId): Boolean = mutex.withLock { store.remove(id) != null }
}
