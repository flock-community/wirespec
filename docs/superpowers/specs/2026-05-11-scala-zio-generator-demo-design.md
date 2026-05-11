# scala-zio: kotest-bridge generator demo

**Status:** Approved (design)
**Date:** 2026-05-11
**Author:** Willem Veelenturf

## Goal

Extend `examples/scala-zio` to show Scala developers how the Wirespec-generated
`*Generator.scala` factories are driven from Scala code, using the existing
Kotlin `kotestWirespecScalaGenerator(...)` bridge as the random-data engine.

The follow-up note in `KotestWirespecScalaGeneratorJvmTest.kt` ("Wider variant
coverage is left for an integration test against `examples/scala-zio`") is
explicitly *not* what we're doing here — wider variant coverage stays in the
kotest module's own tests. This change is a **demo** for example readers, not
an extension of adapter coverage.

## Non-goals

- No property-based testing (no `checkAll`, no ScalaCheck integration).
- No Circe round-trip assertions, no server route property tests.
- No changes to handlers, server, client, or routing in the example.
- No Kotlin sources in the sbt project; no sbt-kotlin plugin.
- No custom `@Generator(...)` registrations in the demo — just defaults.

The user picked the "Just generator usage" option during brainstorming. That
sets the ceiling: a small, readable demo that compiles, runs under `sbt test`,
and shows the seam.

## Architecture

A Scala-only sbt project consumes the Kotlin bridge as a JVM library at test
scope. Two new Scala test files (one helper, one spec) and two lines of
`build.sbt` are the entire change.

```
examples/scala-zio/
├── build.sbt                                  ← +3 lines (resolver + dep)
├── gen.sh                                     ← unchanged (already --shared)
└── src/test/scala/example/
    ├── GuruClientSpec.scala                   ← unchanged
    ├── GuruServerSpec.scala                   ← unchanged
    ├── KotestBridge.scala                     ← NEW (Scala facade, ~15 LOC)
    └── GeneratorSpec.scala                    ← NEW (ZIO Test demo, ~25 LOC)
```

### Data flow

```
GeneratorSpec
  └── KotestBridge.generator(seed)            (Scala test code)
        └── KotestWirespecScalaGeneratorKt    (Kotlin static, in kotest-jvm)
              └── WirespecScalaGeneratorAdapter.create(kotestGenerator(seed))
                    ├── kotestGenerator(seed) → KotestWirespecGenerator
                    └── Proxy → community.flock.wirespec.scala.Wirespec.Generator
                                  (reflective lookup, fails fast if missing)
        └── (cast Any → Wirespec.Generator)
  └── APIGenerator.generate(gen, List.empty)  (generated Scala code)
        └── new API(added = gen.generate(path, GeneratorFieldString(...)), ...)
  └── ZIO Test assertions
```

The cast at the Scala side is unavoidable: the Kotlin module has zero
compile-time Scala dependency, so its factory's static return type is `Any`.
`KotestBridge` hides it so user-facing code stays clean.

## Components

### `KotestBridge.scala` (new)

```scala
package example

import community.flock.wirespec.integration.kotest.{
  KotestWirespecGeneratorBuilder, KotestWirespecScalaGeneratorKt
}
import community.flock.wirespec.scala.Wirespec
import kotlin.Unit
import kotlin.jvm.functions.Function1

/** Scala-side facade for `kotestWirespecScalaGenerator(...)`. Hides the
  * Kotlin `Function1<Builder, Unit>` block parameter and the `Any` cast. */
object KotestBridge {

  /** Generator with no custom `@Generator(...)` registrations. */
  def generator(seed: Long = 0L): Wirespec.Generator =
    KotestWirespecScalaGeneratorKt
      .kotestWirespecScalaGenerator(seed, NoOp)
      .asInstanceOf[Wirespec.Generator]

  private val NoOp: Function1[KotestWirespecGeneratorBuilder, Unit] =
    new Function1[KotestWirespecGeneratorBuilder, Unit] {
      override def invoke(b: KotestWirespecGeneratorBuilder): Unit = Unit.INSTANCE
    }
}
```

**Why a helper at all** instead of inlining at every call site: the
`Function1[Builder, Unit]` is awkward in Scala, and the `asInstanceOf` cast
should not appear in spec source. The facade is small enough to read in one
sitting, and the spec reads naturally because of it.

**Why no overload taking a Scala `Builder => Unit` block:** the demo does not
register any custom generators (see Non-goals). Adding the overload "in case
someone wants it" would be YAGNI. A future test that needs `register(...)`
can add it then.

### `GeneratorSpec.scala` (new)

```scala
package example

import community.flock.wirespec.generated.generator.*
import community.flock.wirespec.generated.model.*
import zio.test.*

object GeneratorSpec extends ZIOSpecDefault {

  override def spec: Spec[Any, Any] = suite("GeneratorSpec")(
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
    test("Different seeds produce different APIs") {
      val a = APIGenerator.generate(KotestBridge.generator(seed = 1L), List.empty)
      val b = APIGenerator.generate(KotestBridge.generator(seed = 2L), List.empty)
      assertTrue(a != b)
    },
  )
}
```

**Why three tests, not one:** the three together tell a story that one
doesn't — (1) the bridge plus a generator yields a populated value, (2) a
second model type works the same way, (3) determinism varies with the seed.
A single test would only show (1).

**Why `assertTrue(m != null)`:** the test exists to demonstrate the call
shape, not to verify generator quality. The trivial predicate is intentional
and is commented inline so a reader understands the choice.

### `build.sbt` (modified)

Add at the top of the `settings` block (or anywhere — sbt key order is
free):

```scala
resolvers += Resolver.mavenLocal,
libraryDependencies +=
  "community.flock.wirespec.integration" % "kotest-jvm" % "0.0.0-SNAPSHOT" % Test,
```

**Coordinate notes:**

- `%` (single), not `%%`: the kotest module is a plain JVM jar, not
  Scala-cross-compiled.
- `0.0.0-SNAPSHOT` matches the rest of the repo's local publication
  convention (see `gradle/libs.versions.toml`: `default = "0.0.0-SNAPSHOT"`).
- `Test` scope: the dep does not need to be on the runtime classpath of
  `GuruServer`.
- Transitives (`kotest-property-jvm`, `kotest-property-arbs-jvm`,
  `kotlin-rgxgen-jvm`, `wirespec-jvm`) are declared in the kotest module's
  POM and resolved automatically.

## Prerequisites for users

The kotest module must be present in the local Maven repo before
`sbt test` resolves the new dep. From the repo root:

```bash
./gradlew :src:integration:kotest:publishToMavenLocal
```

Then in `examples/scala-zio`:

```bash
sbt test
```

This is the same workflow already documented in
`src/integration/kotest/README.md` for Kotlin/Java consumers.

## Error handling

The single failure mode worth naming is **missing `Wirespec.scala` on the
classpath**. The bridge already raises a precise `IllegalStateException`
pointing at the cause:

> Scala-emitted Wirespec.scala not found on classpath. Run your codegen
> with --emit-shared and make sure the generated source set is on the test
> compile/runtime classpath.

`gen.sh` runs `--shared` unconditionally and `build.sbt` adds
`target/generated-sources` to `managedSourceDirectories`, so this should not
fire in the example. The error message is enough on its own; the demo does
not add wrapper assertions.

## Testing

The spec runs under `sbt test` alongside the existing
`GuruServerSpec` and `GuruClientSpec`. Success criteria:

- `sbt clean test` passes after `:src:integration:kotest:publishToMavenLocal`.
- The new spec discovers and runs three tests under
  `example.GeneratorSpec`.
- No deprecation or unused-import warnings introduced.

There is no separate test suite for `KotestBridge`. It is six lines of
delegation; `GeneratorSpec` exercises it transitively.

## Open questions

None.

## Decisions log

- **Scala-only sources, no Kotlin in the sbt project.** Chosen during
  brainstorming. Alternative (Mixed sbt + Kotlin sources running native
  kotest `StringSpec`) was rejected for plumbing cost.
- **Minimal demo, no property testing.** Chosen during brainstorming.
  Property-style coverage of Circe round-trips and server routes was
  considered and explicitly dropped.
- **`Resolver.mavenLocal` rather than a remote repo.** Matches the existing
  publication pattern in this monorepo. A future change can swap this for a
  proper Maven Central coordinate once the kotest module is published.
