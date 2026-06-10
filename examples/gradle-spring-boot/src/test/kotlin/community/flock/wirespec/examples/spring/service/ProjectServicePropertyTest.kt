package community.flock.wirespec.examples.spring.service

import community.flock.wirespec.examples.spring.generated.generator.ProjectInputGenerator
import community.flock.wirespec.examples.spring.generated.model.Project
import community.flock.wirespec.examples.spring.generated.model.ProjectInput
import community.flock.wirespec.examples.spring.repository.MemberRepository
import community.flock.wirespec.examples.spring.repository.ProjectRepository
import community.flock.wirespec.examples.spring.testutil.TestGenerators
import community.flock.wirespec.integration.kotest.kotestWirespecKotlinGenerator
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Property-based companion to [ProjectServiceTest]. Drives the IR-emitted
 * `ProjectInputGenerator` through `kotestWirespecKotlinGenerator` to produce many
 * random `ProjectInput` values per property, asserting service-layer
 * invariants over each one.
 */
class ProjectServicePropertyTest {

    private lateinit var memberRepository: MemberRepository
    private lateinit var service: ProjectService

    @BeforeTest
    fun setUp() {
        // Fresh in-memory state per @Test method. Iterations within a single
        // property share state, which is fine — each iteration operates on
        // its own service-generated id.
        memberRepository = MemberRepository()
        service = ProjectService(ProjectRepository(), memberRepository)
    }

    // The generator draws null for ~20% of nullable paths, so `input.owner`
    // may be absent; the service then resolves `ownerId` against the member
    // repository. Seed it so the lookup succeeds.
    private suspend fun createProject(input: ProjectInput): Project {
        if (input.owner == null) {
            memberRepository.save(TestGenerators.member(id = input.ownerId.value))
        }
        return service.create(input)
    }

    /**
     * Bridge: a kotest `Arb` that draws a fresh per-iteration seed from the
     * `RandomSource` and feeds it to `kotestWirespecKotlinGenerator`, which then
     * drives the IR-emitted `ProjectInputGenerator`. No schema duplication —
     * the Arb is a thin wrapper around the existing generator.
     */
    private val projectInputArb: Arb<ProjectInput> = arbitrary { rs ->
        val gen = kotestWirespecKotlinGenerator(seed = rs.random.nextLong())
        ProjectInputGenerator.generate(gen, emptyList())
    }

    // 50 iterations is enough for variety + shrinking on this in-memory
    // service. Default kotest is 1000; that's overkill here.
    private val config = PropTestConfig(iterations = 50)

    @Test
    fun `create then get returns the same project`() = runTest {
        checkAll(config, projectInputArb) { input ->
            val created = createProject(input)
            val fetched = service.get(created.id)

            assertNotNull(fetched)
            assertEquals(created, fetched)
            assertEquals(input.name, fetched.name)
            assertEquals(input.ownerId, fetched.ownerId)
        }
    }

    @Test
    fun `update replaces fields but keeps the id`() = runTest {
        checkAll(config, projectInputArb, projectInputArb) { original, replacement ->
            val created = createProject(original)
            val updated = service.update(created.id, replacement)

            assertNotNull(updated)
            assertEquals(created.id, updated.id)
            assertEquals(replacement.name, updated.name)
            assertEquals(replacement.ownerId, updated.ownerId)
        }
    }

    @Test
    fun `delete then get returns null`() = runTest {
        checkAll(config, projectInputArb) { input ->
            val created = createProject(input)

            assertTrue(service.delete(created.id))
            assertNull(service.get(created.id))
        }
    }
}
