package community.flock.wirespec.examples.spring.service

import community.flock.wirespec.examples.spring.generated.model.Project
import community.flock.wirespec.examples.spring.generated.model.ProjectId
import community.flock.wirespec.examples.spring.generated.model.ProjectInput
import community.flock.wirespec.examples.spring.repository.MemberRepository
import community.flock.wirespec.examples.spring.repository.ProjectRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProjectService(
    private val repository: ProjectRepository,
    private val memberRepository: MemberRepository,
) {

    suspend fun list(): List<Project> = repository.findAll()

    suspend fun get(id: ProjectId): Project? = repository.findById(id)

    suspend fun create(input: ProjectInput): Project {
        val owner = input.owner
            ?: memberRepository.findById(input.ownerId)
            ?: error("Member ${input.ownerId.value} not found")
        return repository.save(
            Project(
                id = ProjectId(UUID.randomUUID().toString()),
                ref = input.ref,
                name = input.name,
                description = input.description,
                owner = owner,
                ownerId = input.ownerId,
                tags = emptyMap(),
            ),
        )
    }

    suspend fun update(id: ProjectId, input: ProjectInput): Project? {
        val existing = repository.findById(id) ?: return null
        return repository.save(
            existing.copy(
                name = input.name,
                description = input.description,
                ownerId = input.ownerId,
            ),
        )
    }

    suspend fun delete(id: ProjectId): Boolean = repository.delete(id)
}
