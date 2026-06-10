package community.flock.wirespec.examples.spring.service

import community.flock.wirespec.examples.spring.generated.model.Project
import community.flock.wirespec.examples.spring.generated.model.ProjectId
import community.flock.wirespec.examples.spring.generated.model.ProjectInput
import community.flock.wirespec.examples.spring.repository.MemberRepository
import community.flock.wirespec.examples.spring.repository.ProjectRepository
import community.flock.wirespec.examples.spring.testutil.TestGenerators
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProjectServiceTest {

    private val memberRepository = MemberRepository()
    private val service = ProjectService(ProjectRepository(), memberRepository)

    // The generator draws null for ~20% of nullable paths, so `input.owner`
    // may be absent; the service then resolves `ownerId` against the member
    // repository. Seed it so the lookup succeeds.
    private suspend fun createProject(input: ProjectInput): Project {
        if (input.owner == null) {
            memberRepository.save(TestGenerators.member(id = input.ownerId.value))
        }
        return service.create(input)
    }

    @Test
    fun `creates and lists projects`() = runTest {
        val input = TestGenerators.projectInput(seed = 1L)

        val created = createProject(input)
        val all = service.list()

        assertEquals(1, all.size)
        assertEquals(input.name, all.single().name)
        assertEquals(input.ownerId, all.single().ownerId)
        assertEquals(created.id, all.single().id)
    }

    @Test
    fun `updates an existing project`() = runTest {
        val original = TestGenerators.projectInput(seed = 2L)
        val replacement = TestGenerators.projectInput(seed = 3L)
        val created = createProject(original)

        val updated = service.update(created.id, replacement)

        assertNotNull(updated)
        assertEquals(replacement.name, updated.name)
        assertEquals(replacement.description, updated.description)
        assertEquals(replacement.ownerId, updated.ownerId)
        assertEquals(created.id, updated.id)
    }

    @Test
    fun `update returns null when project does not exist`() = runTest {
        val missing = ProjectId("00000000-0000-0000-0000-000000000000")
        val updated = service.update(missing, TestGenerators.projectInput(seed = 4L))

        assertNull(updated)
    }

    @Test
    fun `deletes a project`() = runTest {
        val created = createProject(TestGenerators.projectInput(seed = 5L))

        assertTrue(service.delete(created.id))
        assertNull(service.get(created.id))
    }
}
