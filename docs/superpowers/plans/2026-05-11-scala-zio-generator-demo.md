# scala-zio: kotest-bridge generator demo — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend `examples/scala-zio` with a minimal demo (one Scala helper, one ZIO Test spec, two `build.sbt` lines) showing how a Scala caller drives the generated `*Generator.scala` factories through `kotestWirespecScalaGenerator(...)`.

**Architecture:** Scala-only sbt project, with the Kotlin `kotest-jvm` module added as a test-scope JVM dependency from `mavenLocal`. A small `KotestBridge` object hides the `Function1<Builder, Unit>` block parameter and the `Any → Wirespec.Generator` cast; a `GeneratorSpec` runs three short tests that exercise `APIGenerator` and `MetricsGenerator`. No Kotlin sources in the sbt project; no property testing; no Circe / server route hooks.

**Tech Stack:** Scala 3.3.4, sbt, ZIO Test 2.1.14, the Wirespec `kotest-jvm` integration artifact (`community.flock.wirespec.integration:kotest-jvm:0.0.0-SNAPSHOT`), Java 17.

**Spec:** `docs/superpowers/specs/2026-05-11-scala-zio-generator-demo-design.md`

---

## File Structure

| Path | Status | Responsibility |
|---|---|---|
| `examples/scala-zio/build.sbt` | modify | Add `Resolver.mavenLocal` + the test-scope `kotest-jvm` dep. |
| `examples/scala-zio/src/test/scala/example/KotestBridge.scala` | create | Scala facade over `KotestWirespecScalaGeneratorKt.kotestWirespecScalaGenerator(...)`. Hides the Kotlin `Function1` block and the `Any` cast. |
| `examples/scala-zio/src/test/scala/example/GeneratorSpec.scala` | create | ZIO Test spec with three tests: `APIGenerator` non-null fields, `MetricsGenerator` runs to completion, two seeds produce different `API`s. |

Nothing else changes. `gen.sh` already runs `--shared`, so `Wirespec.scala` is on the test classpath already.

---

### Task 1: Publish the kotest module to mavenLocal

**Files:**
- None modified — this step ensures the test dep declared in Task 2 will resolve.

The kotest module has been edited frequently on the current branch (`ir-arbitrary`). The local Maven cache may be stale, so re-publish before adding the sbt dep.

- [ ] **Step 1: Publish from the repo root**

```bash
cd /Users/wilmveel/Projects/wirespec
./gradlew :src:integration:kotest:publishToMavenLocal
```

Expected: `BUILD SUCCESSFUL`. Two artifacts land in
`~/.m2/repository/community/flock/wirespec/integration/kotest-jvm/0.0.0-SNAPSHOT/`:
the `.jar` and the `.pom`.

- [ ] **Step 2: Verify the JAR is fresh**

```bash
ls -la ~/.m2/repository/community/flock/wirespec/integration/kotest-jvm/0.0.0-SNAPSHOT/kotest-jvm-0.0.0-SNAPSHOT.jar
```

Expected: a file whose mtime is from this `publishToMavenLocal` run (within the last minute).

No commit — this step produces nothing tracked by git.

---

### Task 2: Wire the test-scope dependency into `build.sbt`

**Files:**
- Modify: `examples/scala-zio/build.sbt`

- [ ] **Step 1: Add `Resolver.mavenLocal` and the `kotest-jvm` dep**

Replace the `libraryDependencies ++= Seq(...)` block in `examples/scala-zio/build.sbt` so it includes the new dep, and add `resolvers` immediately above it.

Find this block (currently lines 12-21):

```scala
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-http" % zioHttpVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-http-testkit" % zioHttpVersion % Test,
    ),
```

Replace it with:

```scala
    resolvers += Resolver.mavenLocal,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-http" % zioHttpVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-http-testkit" % zioHttpVersion % Test,
      // Kotlin bridge providing kotestWirespecScalaGenerator(...). Pure JVM
      // artifact (no Scala suffix); transitives are declared in its POM.
      "community.flock.wirespec.integration" % "kotest-jvm" % "0.0.0-SNAPSHOT" % Test,
    ),
```

Note the single `%` (not `%%`) — the kotest module is not Scala-cross-compiled.

- [ ] **Step 2: Verify sbt resolves the new dep**

```bash
cd /Users/wilmveel/Projects/wirespec/examples/scala-zio
sbt update
```

Expected: `[success]` final line, no `unresolved dependency` errors. The transitives `kotest-property-jvm`, `kotest-property-arbs-jvm`, `kotlin-rgxgen-jvm`, and `wirespec-jvm` are pulled automatically (they are declared in the kotest-jvm POM).

If sbt reports `not found: community.flock.wirespec.integration#kotest-jvm`, Task 1 was skipped — re-run it.

- [ ] **Step 3: Commit**

```bash
cd /Users/wilmveel/Projects/wirespec
git add examples/scala-zio/build.sbt
git commit -m "build(examples/scala-zio): add kotest-jvm test dep

Pulls the Kotlin kotestWirespecScalaGenerator(...) bridge from mavenLocal
so an upcoming Scala demo can drive the generated *Generator.scala
factories with kotest-backed random data.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Write the failing test (TDD red phase)

**Files:**
- Create: `examples/scala-zio/src/test/scala/example/GeneratorSpec.scala`

We write the first test before the helper exists so the compile failure proves the test is wired up. The other two tests are added after the helper is in place (Tasks 5 and 6) — keeping the first compile failure isolated makes the red→green transition unambiguous.

- [ ] **Step 1: Create `GeneratorSpec.scala` with one test**

```scala
package example

import community.flock.wirespec.generated.generator.*
import community.flock.wirespec.generated.model.*
import zio.*
import zio.test.*

object GeneratorSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("GeneratorSpec")(
    test("APIGenerator produces non-null required fields") {
      val gen = KotestBridge.generator(seed = 1L)
      val api: API = APIGenerator.generate(gen, List.empty)
      assertTrue(
        api.added != null,
        api.preferred != null,
        api.versions != null,
      )
    },
  )
}
```

The `Spec[TestEnvironment & Scope, Any]` annotation matches the existing `GuruServerSpec` / `GuruClientSpec` style. Pure `test(...) { TestResult }` blocks don't introduce environment requirements, so the type is satisfied via `Spec`'s contravariant `R` parameter.

- [ ] **Step 2: Run sbt — expect compile failure**

```bash
cd /Users/wilmveel/Projects/wirespec/examples/scala-zio
sbt Test/compile
```

Expected: compile fails with `Not found: KotestBridge` (or equivalent Scala 3 error pointing at `example.KotestBridge`). This is the red phase. Do **not** commit yet.

---

### Task 4: Implement `KotestBridge` to make Task 3 compile and pass

**Files:**
- Create: `examples/scala-zio/src/test/scala/example/KotestBridge.scala`

- [ ] **Step 1: Create `KotestBridge.scala`**

```scala
package example

import community.flock.wirespec.integration.kotest.{
  KotestWirespecGeneratorBuilder,
  KotestWirespecScalaGeneratorKt
}
import community.flock.wirespec.scala.Wirespec
import kotlin.Unit
import kotlin.jvm.functions.Function1

/** Scala-side facade for `kotestWirespecScalaGenerator(...)`. Hides the
  * Kotlin `Function1<Builder, Unit>` block parameter and the `Any` cast
  * so spec source stays readable. */
object KotestBridge {

  /** Generator with no custom `@Generator(...)` registrations. */
  def generator(seed: Long = 0L): Wirespec.Generator =
    KotestWirespecScalaGeneratorKt
      .kotestWirespecScalaGenerator(seed, NoOp)
      .asInstanceOf[Wirespec.Generator]

  private val NoOp: Function1[KotestWirespecGeneratorBuilder, Unit] =
    new Function1[KotestWirespecGeneratorBuilder, Unit] {
      override def invoke(b: KotestWirespecGeneratorBuilder): Unit =
        Unit.INSTANCE
    }
}
```

- [ ] **Step 2: Compile and run the spec — expect the single test to pass**

```bash
cd /Users/wilmveel/Projects/wirespec/examples/scala-zio
sbt "testOnly example.GeneratorSpec"
```

Expected: `+ APIGenerator produces non-null required fields` and `1 test passing`. Status line ends with `[success]`.

If the test fails with `IllegalStateException: Scala-emitted Wirespec.scala not found on classpath`, the generated sources aren't on the classpath. Verify `examples/scala-zio/target/generated-sources/community/flock/wirespec/scala/Wirespec.scala` exists; if it doesn't, run `bash examples/scala-zio/gen.sh` and retry.

- [ ] **Step 3: Commit**

```bash
cd /Users/wilmveel/Projects/wirespec
git add examples/scala-zio/src/test/scala/example/KotestBridge.scala examples/scala-zio/src/test/scala/example/GeneratorSpec.scala
git commit -m "test(examples/scala-zio): demo Scala caller using kotest bridge

Adds KotestBridge — a tiny Scala facade over the Kotlin
KotestWirespecScalaGeneratorKt.kotestWirespecScalaGenerator(...) static —
and one GeneratorSpec test that drives APIGenerator.generate(...) with
the resulting Wirespec.Generator. Two more tests in follow-up commits.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: Add the second test (different model)

**Files:**
- Modify: `examples/scala-zio/src/test/scala/example/GeneratorSpec.scala`

- [ ] **Step 1: Add the `MetricsGenerator` test**

Find this block:

```scala
    test("APIGenerator produces non-null required fields") {
      val gen = KotestBridge.generator(seed = 1L)
      val api: API = APIGenerator.generate(gen, List.empty)
      assertTrue(
        api.added != null,
        api.preferred != null,
        api.versions != null,
      )
    },
  )
```

Replace it with:

```scala
    test("APIGenerator produces non-null required fields") {
      val gen = KotestBridge.generator(seed = 1L)
      val api: API = APIGenerator.generate(gen, List.empty)
      assertTrue(
        api.added != null,
        api.preferred != null,
        api.versions != null,
      )
    },
    test("MetricsGenerator returns a populated Metrics") {
      val gen = KotestBridge.generator(seed = 1L)
      val m: Metrics = MetricsGenerator.generate(gen, List.empty)
      // Smoke: the generator runs to completion and produces a non-null
      // instance. Field-level invariants would need property coverage,
      // which is out of scope.
      assertTrue(m != null)
    },
  )
```

- [ ] **Step 2: Run and verify the new test passes**

```bash
cd /Users/wilmveel/Projects/wirespec/examples/scala-zio
sbt "testOnly example.GeneratorSpec"
```

Expected: `2 tests passing`, both green.

If sbt reports `Not found: MetricsGenerator`, `gen.sh` did not produce one for this OpenAPI source. Run `find target/generated-sources -name 'MetricsGenerator.scala'` to confirm and re-run `bash gen.sh` if needed.

- [ ] **Step 3: Commit**

```bash
cd /Users/wilmveel/Projects/wirespec
git add examples/scala-zio/src/test/scala/example/GeneratorSpec.scala
git commit -m "test(examples/scala-zio): cover MetricsGenerator via the kotest bridge

Shows that a second generated factory is driven by the same Wirespec.Generator
the bridge produces; deliberately uses a non-null smoke assertion since
field-level invariants are out of scope for the demo.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 6: Add the third test (seed determinism)

**Files:**
- Modify: `examples/scala-zio/src/test/scala/example/GeneratorSpec.scala`

- [ ] **Step 1: Add the seed-varies test**

Find this block:

```scala
    test("MetricsGenerator returns a populated Metrics") {
      val gen = KotestBridge.generator(seed = 1L)
      val m: Metrics = MetricsGenerator.generate(gen, List.empty)
      // Smoke: the generator runs to completion and produces a non-null
      // instance. Field-level invariants would need property coverage,
      // which is out of scope.
      assertTrue(m != null)
    },
  )
```

Replace it with:

```scala
    test("MetricsGenerator returns a populated Metrics") {
      val gen = KotestBridge.generator(seed = 1L)
      val m: Metrics = MetricsGenerator.generate(gen, List.empty)
      // Smoke: the generator runs to completion and produces a non-null
      // instance. Field-level invariants would need property coverage,
      // which is out of scope.
      assertTrue(m != null)
    },
    test("Different seeds produce different APIs") {
      val a = APIGenerator.generate(KotestBridge.generator(seed = 1L), List.empty)
      val b = APIGenerator.generate(KotestBridge.generator(seed = 2L), List.empty)
      assertTrue(a != b)
    },
  )
```

- [ ] **Step 2: Run all three tests**

```bash
cd /Users/wilmveel/Projects/wirespec/examples/scala-zio
sbt "testOnly example.GeneratorSpec"
```

Expected: `3 tests passing`.

If the third test fails (`a == b`), the bridge is producing identical values regardless of seed — that would point at a real bug in `kotestWirespecScalaGenerator(...)` and should be surfaced as a finding, not patched over.

- [ ] **Step 3: Commit**

```bash
cd /Users/wilmveel/Projects/wirespec
git add examples/scala-zio/src/test/scala/example/GeneratorSpec.scala
git commit -m "test(examples/scala-zio): assert different seeds produce different APIs

Completes the demo: closes the third leg of the story (the bridge respects
the seed) so an example reader can see both the call shape and that
determinism is wired correctly.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

### Task 7: Full-test verification

**Files:**
- None modified.

This task confirms the existing `GuruServerSpec` and `GuruClientSpec` still pass alongside the new spec — i.e. the dep additions and resolver change have no collateral effect.

- [ ] **Step 1: Run the full sbt test target**

```bash
cd /Users/wilmveel/Projects/wirespec/examples/scala-zio
sbt clean test
```

Expected: `[success]` at the bottom. The summary lists `GuruServerSpec`, `GuruClientSpec`, and `GeneratorSpec` all green. Total test count goes up by exactly 3 (the new ones).

If anything in `GuruServerSpec` or `GuruClientSpec` regressed, stop here and investigate before claiming completion — the diff is small enough that any regression points at the dep change.

- [ ] **Step 2: No commit needed**

This task is verification only.

---

## Out of scope (do not implement)

These are intentionally excluded — see `docs/superpowers/specs/2026-05-11-scala-zio-generator-demo-design.md` § Non-goals:

- Property-based testing (`checkAll`, ScalaCheck, ZIO Test `check`).
- Circe round-trip assertions.
- Server route property tests.
- Custom `@Generator(...)` Arb registrations from Scala (no `register(...)` block).
- Kotlin sources in the sbt project; sbt-kotlin plugin.
- A separate test for `KotestBridge` (`GeneratorSpec` exercises it transitively).

If a follow-up wants any of these, it should be a separate spec.
