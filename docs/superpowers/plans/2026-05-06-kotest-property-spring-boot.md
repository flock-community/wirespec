# Kotest property-based test in `gradle-spring-boot` — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a single new test class — `ProjectServicePropertyTest` — that exercises three `ProjectService` invariants over 50 random `ProjectInput` values produced by the IR-emitted `ProjectInputGenerator` driven through `kotestWirespecGenerator`.

**Architecture:** One new test file. JUnit 5 `@Test` methods + `runTest { checkAll(...) }`. Bridge from kotest to the IR-emitted generator via an inline `arbitrary { rs -> ... }` `Arb<ProjectInput>`. No new dependencies, no production-code changes.

**Tech Stack:** Kotlin, JUnit 5, `kotlin.test`, `kotlinx-coroutines-test`, `kotest-property` (already on the test classpath), `wirespec-integration-kotest` (already on the test classpath), Spring Boot (production code untouched).

---

## File Structure

| Path | Action | Responsibility |
|------|--------|----------------|
| `examples/gradle-spring-boot/src/test/kotlin/community/flock/wirespec/examples/spring/service/ProjectServicePropertyTest.kt` | Create | Hosts three property tests against `ProjectService`, plus the `Arb<ProjectInput>` bridge. |

No other files are touched. `TestGenerators.kt` is **not** modified — the `Arb` lives inline in the new test class. If a second call site emerges, a generic helper can be promoted then (out of scope for this plan).

## Reference: types and method signatures the test depends on

These already exist in the codebase. Listed here so the implementer doesn't have to spelunk:

- `community.flock.wirespec.examples.spring.generated.model.ProjectInput` — has fields `ref: String`, `name: String`, `description: String?`, `owner: Member?`, `ownerId: MemberId`.
- `community.flock.wirespec.examples.spring.generated.generator.ProjectInputGenerator.generate(generator: Wirespec.Generator, path: List<String>): ProjectInput`
- `community.flock.wirespec.integration.kotest.kotestWirespecGenerator(seed: Long, block: KotestWirespecGeneratorBuilder.() -> Unit = {}): Wirespec.Generator`
- `community.flock.wirespec.examples.spring.service.ProjectService(projects: ProjectRepository, members: MemberRepository)`
  - `suspend fun create(input: ProjectInput): Project`
  - `suspend fun get(id: ProjectId): Project?`
  - `suspend fun update(id: ProjectId, input: ProjectInput): Project?`
  - `suspend fun delete(id: ProjectId): Boolean`
- `io.kotest.property.arbitrary.arbitrary { rs -> ... }` — `rs` is a `RandomSource` exposing `random: kotlin.random.Random`.
- `io.kotest.property.checkAll(config, arb1, arb2, block)` — runs `block` `config.iterations` times.

---

### Task 1: Create test class with the `create-then-get` property

**Files:**
- Create: `examples/gradle-spring-boot/src/test/kotlin/community/flock/wirespec/examples/spring/service/ProjectServicePropertyTest.kt`

This task lays down the test class skeleton (imports, `setUp`, `Arb<ProjectInput>` bridge, `PropTestConfig`) and the first property. The skeleton mirrors the style of the sibling `ProjectServiceTest.kt`.

- [ ] **Step 1: Create the test file with the first property**

Write `examples/gradle-spring-boot/src/test/kotlin/community/flock/wirespec/examples/spring/service/ProjectServicePropertyTest.kt`:

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
```

- [ ] **Step 2: Compile and run the new test**

Run:
```bash
./gradlew :examples:gradle-spring-boot:test --tests "community.flock.wirespec.examples.spring.service.ProjectServicePropertyTest" -i
```

Expected: BUILD SUCCESSFUL, the single test method `create then get returns the same project` passes (50 iterations).

If it fails to compile, double-check the imports — particularly that `io.kotest.property.arbitrary.arbitrary` and `io.kotest.property.checkAll` resolve against the module's existing `kotest-property` dependency.

If it compiles but the assertion fails, that means `ProjectService.create` or `.get` actually has a bug — flag it; do not weaken the assertion to make it pass.

- [ ] **Step 3: Commit**

```bash
git add examples/gradle-spring-boot/src/test/kotlin/community/flock/wirespec/examples/spring/service/ProjectServicePropertyTest.kt
git commit -m "test(example): add ProjectServicePropertyTest with create-then-get property

Drives ProjectInputGenerator through kotestWirespecGenerator to assert the
create-then-get round-trip invariant over 50 random ProjectInput values."
```

---

### Task 2: Add the `update` property

**Files:**
- Modify: `examples/gradle-spring-boot/src/test/kotlin/community/flock/wirespec/examples/spring/service/ProjectServicePropertyTest.kt`

- [ ] **Step 1: Add the second `@Test` method**

After the existing `create then get returns the same project` method, append inside the class:

```kotlin
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
```

- [ ] **Step 2: Run the test class**

Run:
```bash
./gradlew :examples:gradle-spring-boot:test --tests "community.flock.wirespec.examples.spring.service.ProjectServicePropertyTest" -i
```

Expected: both methods pass — `create then get returns the same project` and `update replaces fields but keeps the id`. Each runs 50 iterations.

- [ ] **Step 3: Commit**

```bash
git add examples/gradle-spring-boot/src/test/kotlin/community/flock/wirespec/examples/spring/service/ProjectServicePropertyTest.kt
git commit -m "test(example): add update property to ProjectServicePropertyTest

Asserts that update preserves the project id and replaces name/ownerId
from the replacement input, over 50 random (original, replacement) pairs."
```

---

### Task 3: Add the `delete` property

**Files:**
- Modify: `examples/gradle-spring-boot/src/test/kotlin/community/flock/wirespec/examples/spring/service/ProjectServicePropertyTest.kt`

- [ ] **Step 1: Add the `assertNull` and `assertTrue` imports**

Add to the existing import block (alphabetical order with the other `kotlin.test` imports):

```kotlin
import kotlin.test.assertNull
import kotlin.test.assertTrue
```

- [ ] **Step 2: Add the third `@Test` method**

Append inside the class, after the `update` method:

```kotlin
    @Test
    fun `delete then get returns null`() = runTest {
        checkAll(config, projectInputArb) { input ->
            val created = service.create(input)

            assertTrue(service.delete(created.id))
            assertNull(service.get(created.id))
        }
    }
```

- [ ] **Step 3: Run the test class**

Run:
```bash
./gradlew :examples:gradle-spring-boot:test --tests "community.flock.wirespec.examples.spring.service.ProjectServicePropertyTest" -i
```

Expected: all three methods pass — `create then get…`, `update replaces fields…`, `delete then get returns null`. Each runs 50 iterations.

- [ ] **Step 4: Commit**

```bash
git add examples/gradle-spring-boot/src/test/kotlin/community/flock/wirespec/examples/spring/service/ProjectServicePropertyTest.kt
git commit -m "test(example): add delete property to ProjectServicePropertyTest

Asserts that delete removes a created project so a subsequent get returns
null, over 50 random ProjectInput values."
```

---

### Task 4: Verify full module test suite still green

**Files:** none modified.

This task confirms that adding the property test didn't perturb anything else in the module — sibling `ProjectServiceTest`, controller tests, generator tests.

- [ ] **Step 1: Run the full module test suite**

Run:
```bash
./gradlew :examples:gradle-spring-boot:test
```

Expected: BUILD SUCCESSFUL. All tests pass — `ProjectServicePropertyTest` (3 methods), `ProjectServiceTest`, `TaskServiceTest`, `ProjectControllerTest`, `TaskControllerTest`, `GeneratorTest`.

If any sibling test fails — that's a real regression, not flakiness from the property test (the property test runs in its own `@BeforeTest`-isolated service). Investigate before claiming done.

- [ ] **Step 2: Show the test summary**

Inspect the Gradle output for the test counts. There should be ≥3 more tests than before this plan started (the three new property methods).

If a clean count is needed:
```bash
./gradlew :examples:gradle-spring-boot:test --rerun-tasks 2>&1 | grep -E "tests completed|BUILD"
```

- [ ] **Step 3: No commit needed**

Task 4 makes no file changes — it's a verification gate. If everything passes, the plan is complete.

---

## Self-Review

**Spec coverage check:**
- Goal (one new property test class in gradle-spring-boot exercising service invariants) → covered by Tasks 1-3.
- Three properties (create-then-get, update, delete) → Tasks 1, 2, 3 each implement one.
- Iterations = 50 → set via `config = PropTestConfig(iterations = 50)` in Task 1.
- `arbitrary { rs -> kotestWirespecGenerator(seed = rs.random.nextLong()) }` bridge → defined in Task 1.
- `@BeforeTest` for fresh service per method → defined in Task 1.
- `runTest { checkAll(...) }` per `@Test` method → applied in every property method.
- No `TestGenerators.kt` modifications → confirmed in File Structure table.
- No new dependencies → confirmed; `kotest-property` and the kotest integration are already in `build.gradle.kts:56-58`.
- Verification command from the spec → executed in Task 4 Step 1.

**Placeholder scan:** No "TBD", "TODO", "implement later", or "similar to Task N" patterns. All code blocks are complete. All commands have explicit expected output.

**Type consistency:**
- `Arb<ProjectInput>` named `projectInputArb` — used identically in Tasks 1, 2, 3.
- `config` of type `PropTestConfig(iterations = 50)` — defined in Task 1, reused in Tasks 2 and 3.
- `service` of type `ProjectService` — initialized via `setUp` in Task 1, used everywhere.
- `service.create(input)` returns `Project` (assigned to `created`); `created.id` is `ProjectId`; `service.update(id, input)` returns `Project?` — consistent across all three properties.
- `service.delete(id)` returns `Boolean` — used as `assertTrue(service.delete(...))` in Task 3.
- All field references (`input.name`, `input.ownerId`, `replacement.name`, `replacement.ownerId`, `created.id`, `updated.id`) match the `.ws` definition of `ProjectInput`/`Project`.

No issues found.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-06-kotest-property-spring-boot.md`. Two execution options:

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
