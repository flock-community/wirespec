package community.flock.wirespec.examples.spring.service

import community.flock.wirespec.examples.spring.generated.model.ProjectId
import community.flock.wirespec.examples.spring.generated.model.TaskStatus
import community.flock.wirespec.examples.spring.repository.MemberRepository
import community.flock.wirespec.examples.spring.repository.ProjectRepository
import community.flock.wirespec.examples.spring.repository.TaskRepository
import community.flock.wirespec.examples.spring.testutil.TestGenerators
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskServiceTest {

    private val projectRepository = ProjectRepository()
    private val taskRepository = TaskRepository()
    private val projectService = ProjectService(projectRepository, MemberRepository())
    private val taskService = TaskService(taskRepository, projectRepository)

    private suspend fun seedProject(seed: Long): ProjectId =
        projectService.create(TestGenerators.projectInput(seed)).id

    @Test
    fun `creates a task within a project`() = runTest {
        val projectId = seedProject(seed = 10L)
        val input = TestGenerators.taskInput(seed = 11L).copy(status = TaskStatus.TODO)

        val created = taskService.create(projectId, input)

        assertNotNull(created)
        assertEquals(projectId, created.projectId)
        assertEquals(input.title, created.title)
        assertEquals(TaskStatus.TODO, created.status)
    }

    @Test
    fun `create returns null when project missing`() = runTest {
        val created = taskService.create(
            ProjectId("00000000-0000-0000-0000-000000000000"),
            TestGenerators.taskInput(seed = 12L),
        )

        assertNull(created)
    }

    @Test
    fun `lists tasks filtered by status`() = runTest {
        val projectId = seedProject(seed = 13L)
        TaskStatus.entries.forEachIndexed { idx, status ->
            taskService.create(
                projectId,
                TestGenerators.taskInput(seed = 100L + idx).copy(status = status),
            )
        }

        val inProgress = taskService.list(projectId, TaskStatus.IN_PROGRESS)
        val all = taskService.list(projectId, null)

        assertNotNull(inProgress)
        assertEquals(1, inProgress.size)
        assertEquals(TaskStatus.IN_PROGRESS, inProgress.single().status)
        assertEquals(TaskStatus.entries.size, all!!.size)
    }

    @Test
    fun `deletes a task`() = runTest {
        val projectId = seedProject(seed = 14L)
        val created = taskService.create(projectId, TestGenerators.taskInput(seed = 15L))!!

        assertTrue(taskService.delete(created.id))
        assertNull(taskService.get(created.id))
    }
}
