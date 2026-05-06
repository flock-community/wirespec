# TaskService property-based test — design

**Date:** 2026-05-06
**Scope:** `examples/gradle-spring-boot`

## Goal

Add a property-based companion to the example-based `TaskServiceTest`, mirroring the structure already established by `ProjectServicePropertyTest`. The new test drives the IR-emitted `TaskInputGenerator` (and `ProjectInputGenerator` for fixture seeding) through `kotestWirespecGenerator` to assert service-layer invariants over many random inputs per property.

## Non-goals

- No schema duplication: Arbs are thin wrappers over the existing IR-emitted generators. We do not hand-write field shapes.
- No coverage of `list` filtering or missing-project guards in this test (those remain in the example-based `TaskServiceTest`).
- No changes to production code (`TaskService`, repositories) or to `TestGenerators`.

## File

`src/test/kotlin/community/flock/wirespec/examples/spring/service/TaskServicePropertyTest.kt`

## Fixtures

Fresh in-memory state per `@Test` method, set up in `@BeforeTest`:

```
projectRepository = ProjectRepository()
taskRepository    = TaskRepository()
projectService    = ProjectService(projectRepository, MemberRepository())
taskService       = TaskService(taskRepository, projectRepository)
```

State is shared across iterations within a single property. That is acceptable here because each iteration seeds its own project (fresh, service-generated `ProjectId`) and operates on its own service-generated `TaskId` — iterations do not collide.

## Arbs

Two `Arb`s, both built with `arbitrary { rs -> ... }`, drawing a fresh per-iteration seed from the `RandomSource` and feeding it to `kotestWirespecGenerator`:

```
val projectInputArb: Arb<ProjectInput> = arbitrary { rs ->
    val gen = kotestWirespecGenerator(seed = rs.random.nextLong())
    ProjectInputGenerator.generate(gen, emptyList())
}

val taskInputArb: Arb<TaskInput> = arbitrary { rs ->
    val gen = kotestWirespecGenerator(seed = rs.random.nextLong())
    TaskInputGenerator.generate(gen, emptyList())
}
```

These are private fields on `TaskServicePropertyTest`. They are intentionally not added to `TestGenerators`, which is a seeded factory rather than an Arb factory.

## Iteration count

`private val config = PropTestConfig(iterations = 50)` — matching `ProjectServicePropertyTest`. Default kotest is 1000 which is overkill for in-memory CRUD.

## Properties

### 1. `create then get returns the same task`

```
checkAll(config, projectInputArb, taskInputArb) { projectInput, taskInput ->
    val projectId = projectService.create(projectInput).id
    val created   = taskService.create(projectId, taskInput)
    assertNotNull(created)
    val fetched   = taskService.get(created.id)

    assertNotNull(fetched)
    assertEquals(created, fetched)
    assertEquals(projectId, created.projectId)
    assertEquals(taskInput.title, fetched.title)
    assertEquals(taskInput.description, fetched.description)
    assertEquals(taskInput.status, fetched.status)
    assertEquals(taskInput.assigneeId, fetched.assigneeId)
}
```

### 2. `update replaces fields but keeps id and projectId`

```
checkAll(config, projectInputArb, taskInputArb, taskInputArb) { projectInput, original, replacement ->
    val projectId = projectService.create(projectInput).id
    val created   = taskService.create(projectId, original)
    assertNotNull(created)
    val updated   = taskService.update(created.id, replacement)

    assertNotNull(updated)
    assertEquals(created.id, updated.id)
    assertEquals(created.projectId, updated.projectId)
    assertEquals(replacement.title, updated.title)
    assertEquals(replacement.description, updated.description)
    assertEquals(replacement.status, updated.status)
    assertEquals(replacement.assigneeId, updated.assigneeId)
}
```

### 3. `delete then get returns null`

```
checkAll(config, projectInputArb, taskInputArb) { projectInput, taskInput ->
    val projectId = projectService.create(projectInput).id
    val created   = taskService.create(projectId, taskInput)
    assertNotNull(created)

    assertTrue(taskService.delete(created.id))
    assertNull(taskService.get(created.id))
}
```

## Imports

Same shape as `ProjectServicePropertyTest`, plus:

- `community.flock.wirespec.examples.spring.generated.generator.TaskInputGenerator`
- `community.flock.wirespec.examples.spring.generated.model.TaskInput`
- `community.flock.wirespec.examples.spring.repository.TaskRepository`

## Verification

- `./gradlew :examples:gradle-spring-boot:test --tests "*TaskServicePropertyTest*"` passes.
- Existing tests continue to pass.
