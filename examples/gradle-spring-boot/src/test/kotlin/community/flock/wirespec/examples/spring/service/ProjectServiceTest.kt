package community.flock.wirespec.examples.spring.service

import community.flock.wirespec.examples.spring.generated.model.ProjectId
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

    private val service = ProjectService(ProjectRepository(), MemberRepository())

    @Test
    fun `creates and lists projects`() = runTest {
        val input = TestGenerators.projectInput(seed = 1L)

        val created = service.create(input)
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
        val created = service.create(original)

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
        val created = service.create(TestGenerators.projectInput(seed = 5L))

        assertTrue(service.delete(created.id))
        assertNull(service.get(created.id))
    }
}
