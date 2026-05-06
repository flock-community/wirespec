package community.flock.wirespec.examples.spring.generator

import community.flock.wirespec.examples.spring.testutil.TestGenerators
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GeneratorTest {

    @Test
    fun `@Seed test seed`() {
        val id = "550e8400-e29b-41d4-a716-446655440000"

        val a = TestGenerators.project(id = id, seed = 1234L)
        val b = TestGenerators.project(id = id, seed = 4321L)

        assertNotEquals(a, b)
    }

    @Test
    fun `@Seed lets caller regenerate the same Project from a known id`() {
        val id = "550e8400-e29b-41d4-a716-446655440000"

        val a = TestGenerators.project(id = id)
        val b = TestGenerators.project(id = id)

        assertEquals(id, a.id.value)
        assertEquals(a, b)
    }

    @Test
    fun `@Seed makes sibling fields differ when the id differs`() {
        val a = TestGenerators.project(id = "550e8400-e29b-41d4-a716-446655440000")
        val c = TestGenerators.project(id = "660e8400-e29b-41d4-a716-446655440001")

        assertNotEquals(a.id, c.id)
        assertNotEquals(a.name, c.name)
    }

    @Test
    fun `project picked from ProjectList matches project regenerated from its id`() {
        val list = TestGenerators.projectList()

        list.projects.forEach { fromList ->
            val regenerated = TestGenerators.project(id = fromList.id.value)
            assertEquals(fromList, regenerated)
        }
    }

    @Test
    fun `@Seed on integer field lets caller regenerate the same Task from a known id`() {
        val a = TestGenerators.task(id = 4242L)
        val b = TestGenerators.task(id = 4242L)

        assertEquals(4242L, a.id.value)
        assertEquals(a, b)
    }

    @Test
    fun `@Seed on integer field makes sibling fields differ when the id differs`() {
        val a = TestGenerators.task(id = 100L)
        val c = TestGenerators.task(id = 200L)

        assertNotEquals(a.id, c.id)
        assertNotEquals(a.title, c.title)
    }

    @Test
    fun `task picked from TaskList matches task regenerated from its integer id`() {
        val list = TestGenerators.taskList()

        list.tasks.forEach { fromList ->
            val regenerated = TestGenerators.task(id = fromList.id.value)
            assertEquals(fromList, regenerated)
        }
    }
}