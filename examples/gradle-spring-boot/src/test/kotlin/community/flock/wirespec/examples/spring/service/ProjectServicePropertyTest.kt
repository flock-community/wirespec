package community.flock.wirespec.examples.spring.service

import community.flock.wirespec.examples.spring.generated.generator.ProjectInputGenerator
import community.flock.wirespec.examples.spring.generated.model.ProjectInput
import community.flock.wirespec.examples.spring.repository.MemberRepository
import community.flock.wirespec.examples.spring.repository.ProjectRepository
import community.flock.wirespec.integration.kotest.kotestWirespecGenerator
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Property-based companion to [ProjectServiceTest]. Drives the IR-emitted
 * `ProjectInputGenerator` through `kotestWirespecGenerator` to produce many
 * random `ProjectInput` values per property, asserting service-layer
 * invariants over each one.
 */
class ProjectServicePropertyTest {

    private lateinit var service: ProjectService

    @BeforeTest
    fun setUp() {
        // Fresh in-memory state per @Test method. Iterations within a single
        // property share state, which is fine — each iteration operates on
        // its own service-generated id.
        service = ProjectService(ProjectRepository(), MemberRepository())
    }

    /**
     * Bridge: a kotest `Arb` that draws a fresh per-iteration seed from the
     * `RandomSource` and feeds it to `kotestWirespecGenerator`, which then
     * drives the IR-emitted `ProjectInputGenerator`. No schema duplication —
     * the Arb is a thin wrapper around the existing generator.
     */
    private val projectInputArb: Arb<ProjectInput> = arbitrary { rs ->
        val gen = kotestWirespecGenerator(seed = rs.random.nextLong())
        ProjectInputGenerator.generate(gen, emptyList())
    }

    // 50 iterations is enough for variety + shrinking on this in-memory
    // service. Default kotest is 1000; that's overkill here.
    private val config = PropTestConfig(iterations = 50)

    @Test
    fun `create then get returns the same project`() = runTest {
        checkAll(config, projectInputArb) { input ->
            val created = service.create(input)
            val fetched = service.get(created.id)

            assertNotNull(fetched)
            assertEquals(created, fetched)
            assertEquals(input.name, fetched.name)
            assertEquals(input.ownerId, fetched.ownerId)
        }
    }
}
