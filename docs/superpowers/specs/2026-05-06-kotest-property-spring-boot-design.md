# Kotest property-based test in `gradle-spring-boot` example

**Date:** 2026-05-06
**Status:** Design approved

## Goal

Demonstrate the `wirespec-integration-kotest` integration by adding one
property-based test to the `examples/gradle-spring-boot` module. The test must
exercise real business logic (`ProjectService`), not just the generator, and
must follow the existing test conventions in the module so it serves as a
reusable template.

## Non-goals

- Switching the module to a kotest spec runner (`StringSpec`/`FunSpec`).
- Adding new dependencies — `kotest-property` and `kotest-property-arbs` are
  already on the test classpath.
- Refactoring `TestGenerators.kt`. If a generic `arbFromGenerator { ... }`
  helper proves useful later, it can be promoted then. YAGNI.

## Approach

Add one new test file that reuses the IR-emitted `ProjectInputGenerator` as
the engine for a kotest `Arb<ProjectInput>`. The bridge between the two is a
single `arbitrary { rs -> ... }` block that draws a fresh per-iteration seed
from kotest's `RandomSource` and feeds it to `kotestWirespecGenerator`.

This avoids duplicating any schema knowledge: the `Arb` is just kotest's
calling convention wrapped around the existing generator.

## File

`examples/gradle-spring-boot/src/test/kotlin/community/flock/wirespec/examples/spring/service/ProjectServicePropertyTest.kt`

## Skeleton

```kotlin
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProjectServicePropertyTest {

    private lateinit var service: ProjectService

    @BeforeTest
    fun setUp() {
        service = ProjectService(ProjectRepository(), MemberRepository())
    }

    private val projectInputArb: Arb<ProjectInput> = arbitrary { rs ->
        val gen = kotestWirespecGenerator(seed = rs.random.nextLong())
        ProjectInputGenerator.generate(gen, emptyList())
    }

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

    @Test
    fun `update replaces fields but keeps the id`() = runTest {
        checkAll(config, projectInputArb, projectInputArb) { original, replacement ->
            val created = service.create(original)
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
            val created = service.create(input)
            assertTrue(service.delete(created.id))
            assertNull(service.get(created.id))
        }
    }
}
```

## Properties asserted

| # | Property | Failure indicates |
|---|----------|-------------------|
| 1 | `∀ input. service.get(service.create(input).id)` returns the created project, with `name` and `ownerId` preserved from the input | `create` drops fields, `get` doesn't see writes, or persistence diverges from the create return value |
| 2 | `∀ original, replacement. service.update(create(original).id, replacement)` returns a project whose id matches the original create and whose `name`/`ownerId` come from the replacement | `update` swaps the id, drops replacement fields, or fails to persist |
| 3 | `∀ input. service.delete(create(input).id)` is `true` and `get(id)` is `null` afterward | `delete` returns wrong status or doesn't actually remove |

## Test mechanics

- **Test runner:** JUnit 5 (`useJUnitPlatform()` is already configured) +
  `kotlin.test` assertions, matching the existing `ProjectServiceTest.kt`
  style.
- **Coroutine scope:** `runTest { ... }` wraps each `checkAll`. The service
  methods are `suspend`, so this is required.
- **Iterations:** 50 per property. Default kotest is 1000, but the in-memory
  `ProjectRepository` makes that overkill — 50 is enough for shrinking and
  variety, fast enough for CI. Easy to bump per-property if a regression
  shows up.
- **Service state:** fresh `ProjectService` per `@Test` method via
  `@BeforeTest`. Iterations within a single property share the in-memory
  repository, which is fine because each iteration operates on its own
  freshly-created `id` and the service generates the id (so iterations don't
  collide).

## Why this exercises the integration meaningfully

The `ProjectInput` graph routed through `ProjectInputGenerator` touches every
moving piece of the integration:

- `@Generator("email")` and `@Generator("fullname")` on `Member` fields →
  `Arb.email()` and `Arb.name()` defaults from `DefaultArbs`
- `MemberId` regex → `RgxGen` regex generation
- Optional fields (`description: String?`, `owner: Member?`) →
  `GeneratorFieldNullable` materialization
- Nested `Member` reference → recursive shape generation

If any of those break (e.g. a future regex tightening, a missing
registration, a transform refactor), one of these properties will fail.

## Verification

```bash
./gradlew :examples:gradle-spring-boot:test --tests "*ProjectServicePropertyTest*"
./gradlew :examples:gradle-spring-boot:test  # full suite still green
```

## Out of scope (future work)

- Generator-only property test (`@Seed` round-trip over many ids) — the
  existing `GeneratorTest.kt` covers the cases by example and is easy to
  promote later.
- Controller-level property test via `MockMvc` — could be a follow-up to
  exercise JSON serialization round-trips.
- A reusable `arbFromGenerator { ... }` helper in `TestGenerators.kt` — wait
  for a second call site before extracting.
