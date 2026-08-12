package community.flock.wirespec.examples.spring.repository

import community.flock.wirespec.examples.spring.generated.model.Project
import community.flock.wirespec.examples.spring.generated.model.ProjectId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Repository

@Repository
class ProjectRepository {

    private val mutex = Mutex()
    private val store = mutableMapOf<ProjectId, Project>()

    suspend fun findAll(): List<Project> = mutex.withLock { store.values.toList() }

    suspend fun findById(id: ProjectId): Project? = mutex.withLock { store[id] }

    suspend fun save(project: Project): Project = mutex.withLock {
        store[project.id] = project
        project
    }

    suspend fun delete(id: ProjectId): Boolean = mutex.withLock { store.remove(id) != null }
}
