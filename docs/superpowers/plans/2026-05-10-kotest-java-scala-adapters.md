# Kotest integration for Java- and Scala-emitted code — implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let Kotlin tests drive Java- and Scala-emitted `*Generator.generate(generator, path)` factories with the same kotest property arbs that already work for Kotlin-emitted code, by adding two new sibling factories — `kotestWirespecJavaGenerator(...)` and `kotestWirespecScalaGenerator(...)` — alongside a renamed `kotestWirespecKotlinGenerator(...)`.

**Architecture:** Three pure-Kotlin adapters in `:src:integration:kotest:jvmMain`, all built on the existing commonMain `KotestGenerator`. Java is statically typed against the runtime `community.flock.wirespec.java.Wirespec` (we extend `Wirespec.java` to declare `Generator` + `GeneratorField*` records mirroring the IR-emitted shape). Scala goes through a `java.lang.reflect.Proxy` so the kotest module has zero compile-time Scala dependency; Scala runtime types are resolved at first call from the user's `--emit-shared` output.

**Tech Stack:** Kotlin Multiplatform (JVM target only for the new code), Kotest property-test DSL, Java records, `java.lang.reflect.Proxy`, Scala 3 stdlib (test-scope only, for fixture).

**Spec:** `docs/superpowers/specs/2026-05-10-kotest-java-scala-adapters-design.md`

---

## File Structure

**Modify:**
- `src/integration/wirespec/src/jvmMain/java/community/flock/wirespec/java/Wirespec.java` — add `Generator` interface and 11 `GeneratorField*` records.
- `src/integration/kotest/build.gradle.kts` — add Scala-3 stdlib as a `jvmTest` dependency (for the Scala adapter fixture).
- `src/integration/kotest/README.md` — rename `kotestWirespecGenerator` → `kotestWirespecKotlinGenerator` in examples; add Java and Scala usage sections.

**Rename:**
- `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJvm.kt` → `KotestWirespecKotlinGenerator.kt` (also rename the public function and the internal adapter class).
- `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJvmTest.kt` → `KotestWirespecKotlinGeneratorJvmTest.kt`.

**Create:**
- `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecJavaGenerator.kt` — `kotestWirespecJavaGenerator(...)` factory + `WirespecJavaGeneratorAdapter`.
- `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecScalaGenerator.kt` — `kotestWirespecScalaGenerator(...)` factory + `WirespecScalaGeneratorAdapter` (an `InvocationHandler`).
- `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/ScalaInterop.kt` — reflective Scala ↔ Kotlin conversions, cached via `lazy`.
- `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecJavaGeneratorJvmTest.kt`.
- `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecScalaGeneratorJvmTest.kt`.
- `src/integration/kotest/src/jvmTest/java/community/flock/wirespec/scala/Wirespec.java` — Java fixture standing in for the user's emitted `Wirespec.scala`. Declares `Wirespec.Generator` + minimal `GeneratorField*` records using Scala-3-stdlib types so the Scala adapter's reflective decoding can be exercised without a Scala plugin.

---

## Task 1: Add `Generator` types to the runtime `Wirespec.java`

**Files:**
- Modify: `src/integration/wirespec/src/jvmMain/java/community/flock/wirespec/java/Wirespec.java`

Adds the missing `Generator` interface and 11 `GeneratorField*` records to the published `wirespec-jvm` runtime artifact, mirroring the shape the IR emitter produces under `--emit-shared` (verified by `JavaIrEmitterTest.sharedOutputTest`). Purely additive; no Kotlin metadata bump.

- [ ] **Step 1: Add the new declarations**

Open `src/integration/wirespec/src/jvmMain/java/community/flock/wirespec/java/Wirespec.java`. The file currently ends with `getType(...)` at the bottom (line 52–61). Insert the new declarations *before* `getType`, after `record RawResponse(...)` on line 51. The full insertion is below — paste verbatim.

```java
    sealed interface GeneratorField<T>
        permits GeneratorFieldString, GeneratorFieldInteger, GeneratorFieldNumber,
                GeneratorFieldBoolean, GeneratorFieldBytes, GeneratorFieldEnum,
                GeneratorFieldUnion, GeneratorFieldArray, GeneratorFieldNullable,
                GeneratorFieldShape, GeneratorFieldDict {}
    record GeneratorFieldString(Optional<String> regex, List<Map<String, Object>> annotations)
        implements GeneratorField<String> {}
    record GeneratorFieldInteger(Optional<Long> min, Optional<Long> max, List<Map<String, Object>> annotations)
        implements GeneratorField<Long> {}
    record GeneratorFieldNumber(Optional<Double> min, Optional<Double> max, List<Map<String, Object>> annotations)
        implements GeneratorField<Double> {}
    record GeneratorFieldBoolean(List<Map<String, Object>> annotations)
        implements GeneratorField<Boolean> {}
    record GeneratorFieldBytes(List<Map<String, Object>> annotations)
        implements GeneratorField<byte[]> {}
    record GeneratorFieldEnum(List<String> values, List<Map<String, Object>> annotations, Type type)
        implements GeneratorField<String> {}
    record GeneratorFieldUnion(List<String> variants, List<Map<String, Object>> annotations, Type type)
        implements GeneratorField<String> {}
    record GeneratorFieldArray<T>(java.util.function.Function<List<String>, T> generate)
        implements GeneratorField<List<T>> {}
    record GeneratorFieldNullable<T>(java.util.function.Function<List<String>, T> generate)
        implements GeneratorField<Optional<T>> {}
    record GeneratorFieldShape<T>(Map<String, List<Map<String, Object>>> annotations,
                                  java.util.function.Function<List<String>, T> generate,
                                  Type type)
        implements GeneratorField<T> {}
    record GeneratorFieldDict<V>(java.util.function.Function<List<String>, V> generate)
        implements GeneratorField<Map<String, V>> {}
    interface Generator {
        <T> T generate(List<String> path, GeneratorField<T> field);
    }
```

- [ ] **Step 2: Verify the file compiles**

Run: `./gradlew :src:integration:wirespec:build`
Expected: BUILD SUCCESSFUL. The integration jvm jar now exposes `community.flock.wirespec.java.Wirespec.Generator` and 11 `GeneratorField*` records.

- [ ] **Step 3: Commit**

```bash
git add src/integration/wirespec/src/jvmMain/java/community/flock/wirespec/java/Wirespec.java
git commit -m "feat(integration/wirespec): declare Generator + GeneratorField* in Wirespec.java

Mirror the IR emitter's --emit-shared output so the Java runtime
artifact and user-emitted Wirespec.java are structurally identical.
Purely additive; no Kotlin metadata bump.

Unblocks the kotest Java adapter (next commits)."
```

---

## Task 2: Rename existing Kotlin adapter for symmetry

**Files:**
- Rename: `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJvm.kt` → `KotestWirespecKotlinGenerator.kt`
- Rename: `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJvmTest.kt` → `KotestWirespecKotlinGeneratorJvmTest.kt`

The pre-rename function `kotestWirespecGenerator(...)` becomes `kotestWirespecKotlinGenerator(...)`; internal class `WirespecGeneratorAdapter` becomes `WirespecKotlinGeneratorAdapter`. Test file updated accordingly. Breaking change for direct consumers — acknowledged in the spec.

- [ ] **Step 1: Move the source file**

```bash
git mv src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJvm.kt \
       src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecKotlinGenerator.kt
```

- [ ] **Step 2: Rename the public function and the internal class**

Edit `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecKotlinGenerator.kt`. Replace the file's contents with:

```kotlin
package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec

/**
 * JVM-facing factory for Kotlin-emitted code: returns a `community.flock.wirespec
 * .kotlin.Wirespec.Generator`, which is the contract IR-emitted
 * `*Generator.generate(gen: Wirespec.Generator, …)` factories expect. Internally
 * builds a multiplatform [KotestGenerator] and wraps it in
 * [WirespecKotlinGeneratorAdapter], which translates `Wirespec.GeneratorField*`
 * inputs into the kotest-owned [KotestField] mirror types on each call.
 *
 * ```
 * val gen = kotestWirespecKotlinGenerator(seed = 1L) {
 *     register("orderId") { Arb.uuid().map(java.util.UUID::toString) }
 * }
 * val member = MemberGenerator.generate(gen, emptyList())
 * ```
 *
 * For Java- and Scala-emitted code, see [kotestWirespecJavaGenerator] and
 * [kotestWirespecScalaGenerator] respectively.
 */
fun kotestWirespecKotlinGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Wirespec.Generator = WirespecKotlinGeneratorAdapter(kotestGenerator(seed, block))

/**
 * Bridge between Wirespec's Kotlin `Generator` / `GeneratorField*` (which live
 * in `:src:integration:wirespec`, JVM-only, Kotlin 1.9-pinned for downstream
 * binary compat) and kotest's commonMain `KotestGenerator` / `KotestField*`
 * mirror types. The two hierarchies are 1:1, so translation is a flat `when`
 * with no semantic logic.
 */
internal class WirespecKotlinGeneratorAdapter(private val inner: KotestGenerator) : Wirespec.Generator {

    @Suppress("UNCHECKED_CAST")
    override fun <T> generate(
        path: List<String>,
        field: Wirespec.GeneratorField<T>,
    ): T = inner.generate(path, field.toKotestField() as KotestField<T>) as T

    @Suppress("UNCHECKED_CAST")
    private fun Wirespec.GeneratorField<*>.toKotestField(): KotestField<*> = when (this) {
        is Wirespec.GeneratorFieldString -> KotestFieldString(regex, annotations)
        is Wirespec.GeneratorFieldInteger -> KotestFieldInteger(min, max, annotations)
        is Wirespec.GeneratorFieldNumber -> KotestFieldNumber(min, max, annotations)
        is Wirespec.GeneratorFieldBoolean -> KotestFieldBoolean(annotations)
        is Wirespec.GeneratorFieldBytes -> KotestFieldBytes(annotations)
        is Wirespec.GeneratorFieldEnum -> KotestFieldEnum(values, annotations, type)
        is Wirespec.GeneratorFieldUnion -> KotestFieldUnion(variants, annotations, type)
        is Wirespec.GeneratorFieldArray<*> -> {
            val gen = (this as Wirespec.GeneratorFieldArray<Any>).generate
            KotestFieldArray(gen)
        }
        is Wirespec.GeneratorFieldNullable<*> -> {
            val gen = (this as Wirespec.GeneratorFieldNullable<Any>).generate
            KotestFieldNullable(gen)
        }
        is Wirespec.GeneratorFieldShape<*> -> {
            val shape = this as Wirespec.GeneratorFieldShape<Any>
            KotestFieldShape(shape.annotations, shape.generate, shape.type)
        }
        is Wirespec.GeneratorFieldDict<*> -> {
            val gen = (this as Wirespec.GeneratorFieldDict<Any>).generate
            KotestFieldDict(gen)
        }
    }
}
```

- [ ] **Step 3: Move the test file**

```bash
git mv src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJvmTest.kt \
       src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecKotlinGeneratorJvmTest.kt
```

- [ ] **Step 4: Update test references**

Edit `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecKotlinGeneratorJvmTest.kt`. Two changes:

1. Rename the class declaration from `class KotestWirespecGeneratorJvmTest` to `class KotestWirespecKotlinGeneratorJvmTest`.
2. Replace every call to `kotestWirespecGenerator(` with `kotestWirespecKotlinGenerator(` (4 occurrences, one per test).

- [ ] **Step 5: Verify the rename compiles and tests pass**

Run: `./gradlew :src:integration:kotest:jvmTest`
Expected: BUILD SUCCESSFUL, 4 tests in `KotestWirespecKotlinGeneratorJvmTest` pass, plus the existing commonTest tests.

- [ ] **Step 6: Commit**

```bash
git add src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/ \
        src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/
git commit -m "refactor(integration/kotest): rename Kotlin adapter for symmetry

KotestWirespecGeneratorJvm.kt -> KotestWirespecKotlinGenerator.kt
kotestWirespecGenerator(...)  -> kotestWirespecKotlinGenerator(...)
WirespecGeneratorAdapter      -> WirespecKotlinGeneratorAdapter

Breaking change. Sets up the Kotlin/Java/Scala sibling trio."
```

---

## Task 3: Java adapter — failing test

**Files:**
- Create: `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecJavaGeneratorJvmTest.kt`

A test mirroring the structure of `KotestWirespecKotlinGeneratorJvmTest` but using the Java `Wirespec.GeneratorField*` records added in Task 1. Confirms the adapter dispatches every variant and round-trips `Optional<T>` for the nullable case.

- [ ] **Step 1: Write the failing test**

Create `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecJavaGeneratorJvmTest.kt`:

```kotlin
package community.flock.wirespec.integration.kotest

import community.flock.wirespec.java.Wirespec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import java.util.Optional
import java.util.function.Function
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cover the JVM-only Java `Wirespec.Generator` adapter — the rest of the
 * algorithm is exercised against the kotest-owned `KotestField*` types in
 * commonTest. Here we confirm:
 *   1. `kotestWirespecJavaGenerator(...)` returns something assignable to
 *      the Java Wirespec contract IR-emitted callers depend on.
 *   2. Each `Wirespec.GeneratorField*` Java record survives the round-trip
 *      through `WirespecJavaGeneratorAdapter` into the commonMain algorithm.
 *   3. `GeneratorFieldNullable<T>` returns `Optional<T>` (Java semantics)
 *      rather than the bare `T?` the commonMain algorithm produces.
 */
class KotestWirespecJavaGeneratorJvmTest {

    @Test
    fun `factory returns a Java Wirespec_Generator`() {
        val gen: Wirespec.Generator = kotestWirespecJavaGenerator(seed = 1L)
        assertNotNull(gen)
    }

    @Test
    fun `adapter routes Java Wirespec_GeneratorFieldString through the algorithm`() {
        val gen = kotestWirespecJavaGenerator(seed = 0L) {
            register("orderId") { Arb.constant("ORD-JAVA") }
        }
        val v: String = gen.generate(
            listOf("x"),
            Wirespec.GeneratorFieldString(
                Optional.empty(),
                listOf(mapOf("name" to "Generator", "parameters" to mapOf("default" to "orderId"))),
            ),
        )
        assertEquals("ORD-JAVA", v)
    }

    @Test
    fun `adapter wraps GeneratorFieldNullable result in Optional`() {
        val gen = kotestWirespecJavaGenerator(seed = 0L)
        val nullableField = Wirespec.GeneratorFieldNullable<String>(
            Function { _ -> "value" },
        )
        val v: Optional<String> = gen.generate(listOf("n"), nullableField)
        assertTrue(v.isPresent)
        assertEquals("value", v.get())
    }

    @Test
    fun `adapter handles all Java Wirespec_GeneratorField variants without throwing`() {
        val gen = kotestWirespecJavaGenerator(seed = 0L)

        gen.generate(listOf("s"), Wirespec.GeneratorFieldString(Optional.empty(), emptyList()))
        gen.generate(listOf("i"), Wirespec.GeneratorFieldInteger(Optional.empty(), Optional.empty(), emptyList()))
        gen.generate(listOf("nu"), Wirespec.GeneratorFieldNumber(Optional.empty(), Optional.empty(), emptyList()))
        gen.generate(listOf("b"), Wirespec.GeneratorFieldBoolean(emptyList()))
        gen.generate(listOf("y"), Wirespec.GeneratorFieldBytes(emptyList()))
        gen.generate(
            listOf("e"),
            Wirespec.GeneratorFieldEnum(listOf("A", "B"), emptyList(), String::class.java),
        )
        gen.generate(
            listOf("u"),
            Wirespec.GeneratorFieldUnion(listOf("V1"), emptyList(), String::class.java),
        )
        gen.generate(
            listOf("a"),
            Wirespec.GeneratorFieldArray<String>(Function { _ -> "x" }),
        )
        val nullable: Optional<String> = gen.generate(
            listOf("nul"),
            Wirespec.GeneratorFieldNullable<String>(Function { _ -> "y" }),
        )
        assertTrue(nullable.isPresent)
        gen.generate(
            listOf("sh"),
            Wirespec.GeneratorFieldShape<Map<String, String>>(
                emptyMap(),
                Function { _ -> mapOf("k" to "v") },
                Map::class.java,
            ),
        )
        gen.generate(
            listOf("d"),
            Wirespec.GeneratorFieldDict<String>(Function { _ -> "v" }),
        )
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `./gradlew :src:integration:kotest:jvmTest --tests "community.flock.wirespec.integration.kotest.KotestWirespecJavaGeneratorJvmTest"`
Expected: COMPILATION FAILED — `Unresolved reference: kotestWirespecJavaGenerator`.

---

## Task 4: Java adapter — implementation

**Files:**
- Create: `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecJavaGenerator.kt`

The adapter mirrors `WirespecKotlinGeneratorAdapter` but with Java-flavored field types: `Optional<X>` instead of `X?`, `java.util.function.Function<List<String>, T>` instead of Kotlin lambdas, `java.lang.reflect.Type` instead of `KType`. Critically, `GeneratorFieldNullable.generate` returns `Optional<T>` to the caller, so the adapter wraps the inner result in `Optional.ofNullable(...)`.

- [ ] **Step 1: Write the implementation**

Create `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecJavaGenerator.kt`:

```kotlin
package community.flock.wirespec.integration.kotest

import community.flock.wirespec.java.Wirespec
import java.util.Optional
import kotlin.reflect.typeOf

/**
 * JVM-facing factory for Java-emitted code: returns a `community.flock.wirespec
 * .java.Wirespec.Generator`, which is what Java IR-emitted `*Generator.generate(
 * Wirespec.Generator, …)` factories expect.
 *
 * ```
 * val gen: Wirespec.Generator = kotestWirespecJavaGenerator(seed = 1L) {
 *     register("orderId") { Arb.uuid().map(java.util.UUID::toString) }
 * }
 * val member = MemberGenerator.generate(gen, java.util.List.of())
 * ```
 *
 * Same DSL, default arb catalog, and `@Generator` / `@Seed` semantics as the
 * Kotlin sibling [kotestWirespecKotlinGenerator]. The difference is the
 * concrete `Wirespec.GeneratorField*` shape: Java records use
 * `java.util.Optional` for nullable min/max/regex and `java.util.function
 * .Function` for the `generate` callbacks; nullable fields return
 * `Optional<T>` rather than the bare `T?` the commonMain algorithm produces.
 */
fun kotestWirespecJavaGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Wirespec.Generator = WirespecJavaGeneratorAdapter(kotestGenerator(seed, block))

/**
 * Bridge between the Java `Wirespec.GeneratorField*` records and the kotest
 * commonMain `KotestField*` mirror.
 *
 * Differences from the Kotlin adapter:
 * - `Optional<X>` ⇄ `X?` for `regex` / `min` / `max`.
 * - `java.util.function.Function<List<String>, T>` ⇄ `(List<String>) -> T`.
 * - `KotestField*` carries `kotlin.reflect.KType` for Enum/Union/Shape, but
 *   the kotest impl never reads it; pass `typeOf<Any>()` as a placeholder.
 * - For `GeneratorFieldNullable<T>`, wrap the inner `T?` result in
 *   `Optional.ofNullable(...)` on return.
 */
internal class WirespecJavaGeneratorAdapter(private val inner: KotestGenerator) : Wirespec.Generator {

    @Suppress("UNCHECKED_CAST")
    override fun <T> generate(
        path: List<String>,
        field: Wirespec.GeneratorField<T>,
    ): T = when (field) {
        is Wirespec.GeneratorFieldNullable<*> -> {
            val inner = (field as Wirespec.GeneratorFieldNullable<Any>)
            val value: Any? = this.inner.generate(
                path,
                KotestFieldNullable<Any> { p -> inner.generate.apply(p) },
            )
            Optional.ofNullable(value) as T
        }
        else -> inner.generate(path, field.toKotestField() as KotestField<T>) as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun Wirespec.GeneratorField<*>.toKotestField(): KotestField<*> = when (this) {
        is Wirespec.GeneratorFieldString -> KotestFieldString(regex.orElse(null), annotations)
        is Wirespec.GeneratorFieldInteger -> KotestFieldInteger(min.orElse(null), max.orElse(null), annotations)
        is Wirespec.GeneratorFieldNumber -> KotestFieldNumber(min.orElse(null), max.orElse(null), annotations)
        is Wirespec.GeneratorFieldBoolean -> KotestFieldBoolean(annotations)
        is Wirespec.GeneratorFieldBytes -> KotestFieldBytes(annotations)
        is Wirespec.GeneratorFieldEnum -> KotestFieldEnum(values, annotations, typeOf<Any>())
        is Wirespec.GeneratorFieldUnion -> KotestFieldUnion(variants, annotations, typeOf<Any>())
        is Wirespec.GeneratorFieldArray<*> -> {
            val arr = this as Wirespec.GeneratorFieldArray<Any>
            KotestFieldArray<Any> { p -> arr.generate.apply(p) }
        }
        is Wirespec.GeneratorFieldNullable<*> ->
            error("GeneratorFieldNullable handled in generate(...) above")
        is Wirespec.GeneratorFieldShape<*> -> {
            val shape = this as Wirespec.GeneratorFieldShape<Any>
            KotestFieldShape<Any>(shape.annotations, { p -> shape.generate.apply(p) }, typeOf<Any>())
        }
        is Wirespec.GeneratorFieldDict<*> -> {
            val dict = this as Wirespec.GeneratorFieldDict<Any>
            KotestFieldDict<Any> { p -> dict.generate.apply(p) }
        }
    }
}
```

- [ ] **Step 2: Run the Java tests and verify they pass**

Run: `./gradlew :src:integration:kotest:jvmTest --tests "community.flock.wirespec.integration.kotest.KotestWirespecJavaGeneratorJvmTest"`
Expected: BUILD SUCCESSFUL, all 4 tests in `KotestWirespecJavaGeneratorJvmTest` pass.

- [ ] **Step 3: Run the full kotest jvmTest to make sure nothing else regressed**

Run: `./gradlew :src:integration:kotest:jvmTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecJavaGenerator.kt \
        src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecJavaGeneratorJvmTest.kt
git commit -m "feat(integration/kotest): add kotestWirespecJavaGenerator

Statically-typed adapter from KotestGenerator to community.flock.wirespec
.java.Wirespec.Generator. Wraps GeneratorFieldNullable's result in
Optional.ofNullable on return so Java callers get Optional<T> instead of
the bare T? the commonMain algorithm produces."
```

---

## Task 5: Scala adapter — scaffolded factory + stubbed dispatch

**Files:**
- Create: `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecScalaGenerator.kt`
- Create: `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/ScalaInterop.kt`

Scaffolds the factory and the dispatch object so the next task (the test) compiles. The factory eagerly materializes a `java.lang.reflect.Proxy` against `community.flock.wirespec.scala.Wirespec$Generator`, which is resolved at construction time from whatever the user's `--emit-shared` (or the next task's Java fixture) places on the classpath. `ScalaInterop.dispatch(...)` is intentionally a `TODO` here; Task 7 fills it in once the test exists to drive the implementation.

There is no test in this task: writing one now requires the fixture and the Scala stdlib on the classpath, both of which arrive in Task 6. Compiling the scaffold is the verification step.

- [ ] **Step 1: Write `KotestWirespecScalaGenerator.kt`**

Create `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecScalaGenerator.kt`:

```kotlin
package community.flock.wirespec.integration.kotest

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * JVM-facing factory for Scala-emitted code. Returns a value assignable to
 * `community.flock.wirespec.scala.Wirespec.Generator` at the call site:
 *
 * ```
 * val gen = kotestWirespecScalaGenerator(seed = 1L)
 *     as community.flock.wirespec.scala.Wirespec.Generator
 * val member = MemberGenerator.generate(gen, scala.collection.immutable.List.empty())
 * ```
 *
 * Unlike the Kotlin and Java siblings, this factory has **zero compile-time
 * dependency on Scala types** — the kotest module ships no Scala. The Scala
 * `Wirespec` interface is resolved at construction from the runtime classpath,
 * which the user populates by running their codegen with `--emit-shared` so
 * the generated `Wirespec.scala` lands on the test classpath.
 *
 * If the Scala-emitted `Wirespec` is missing, construction raises
 * `IllegalStateException` pointing at the cause.
 */
fun kotestWirespecScalaGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Any = WirespecScalaGeneratorAdapter.create(kotestGenerator(seed, block))

/**
 * Eager-proxy Scala adapter. The classpath lookup is performed once at factory
 * time so missing-runtime errors surface immediately rather than mid-test.
 */
internal object WirespecScalaGeneratorAdapter {

    fun create(inner: KotestGenerator): Any {
        val cl = Thread.currentThread().contextClassLoader
            ?: javaClass.classLoader
        val generatorIface = try {
            cl.loadClass("community.flock.wirespec.scala.Wirespec\$Generator")
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException(
                "Scala-emitted Wirespec.scala not found on classpath. " +
                    "Run your codegen with --emit-shared and make sure the " +
                    "generated source set is on the test compile/runtime classpath.",
                e,
            )
        }
        val handler = InvocationHandler { _, method, args ->
            ScalaInterop.dispatch(inner, method, args ?: emptyArray())
        }
        return Proxy.newProxyInstance(cl, arrayOf(generatorIface), handler)
    }
}
```

- [ ] **Step 2: Write the `ScalaInterop.kt` stub**

Create `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/ScalaInterop.kt`:

```kotlin
package community.flock.wirespec.integration.kotest

import java.lang.reflect.Method

/**
 * Reflective Scala ↔ Kotlin conversions used by [WirespecScalaGeneratorAdapter].
 *
 * Filled in by Task 7 once a Java fixture is on the test classpath. Until then
 * `dispatch` is intentionally unimplemented so any caller surfaces a clear
 * error rather than silent garbage.
 */
internal object ScalaInterop {

    fun dispatch(inner: KotestGenerator, method: Method, args: Array<Any?>): Any? {
        check(method.name == "generate") {
            "Scala adapter received unexpected method call: ${method.name}"
        }
        TODO("ScalaInterop.dispatch — implemented in Task 7")
    }
}
```

- [ ] **Step 3: Verify the scaffold compiles**

Run: `./gradlew :src:integration:kotest:compileKotlinJvm`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecScalaGenerator.kt \
        src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/ScalaInterop.kt
git commit -m "feat(integration/kotest): scaffold kotestWirespecScalaGenerator

Eager java.lang.reflect.Proxy wrapper that resolves
community.flock.wirespec.scala.Wirespec\$Generator at factory time and
adapts a KotestGenerator to it. ScalaInterop.dispatch is stubbed with
TODO; the reflective field-shape decoding lands once a Java fixture is
on the test classpath (next commit)."
```

---

## Task 6: Scala fixture and end-to-end failing test

**Files:**
- Modify: `src/integration/kotest/build.gradle.kts` — add Scala-3 stdlib as `jvmTest` dep.
- Create: `src/integration/kotest/src/jvmTest/java/community/flock/wirespec/scala/Wirespec.java` — fixture standing in for the user's emitted `Wirespec.scala`.
- Create: `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecScalaGeneratorJvmTest.kt` — exercises the proxy.

The fixture is a Java file in the Scala FQN that declares `Wirespec.Generator` plus the `GeneratorField*` records, using Scala-stdlib types (`scala.collection.immutable.List`, `scala.Option`, `scala.Function1`) for the record components. This is the smallest possible stand-in for emit-shared output — enough to drive the adapter's reflective decoding without applying the Gradle Scala plugin.

- [ ] **Step 1: Add Scala stdlib as a jvmTest dependency**

Edit `src/integration/kotest/build.gradle.kts`. In the `kotlin { sourceSets { jvmTest { dependencies { ... } } } }` block, add:

```kotlin
        jvmTest {
            dependencies {
                implementation(project(":src:integration:wirespec"))
                implementation("org.scala-lang:scala3-library_3:3.3.4")
            }
        }
```

(The first line is already present; only `org.scala-lang:scala3-library_3:3.3.4` is new. Pin to the same Scala version `examples/scala-zio/build.sbt` uses.)

- [ ] **Step 2: Write the Java fixture**

Create `src/integration/kotest/src/jvmTest/java/community/flock/wirespec/scala/Wirespec.java`:

```java
package community.flock.wirespec.scala;

import scala.Function1;
import scala.Option;
import scala.collection.immutable.List;
import scala.collection.immutable.Map;
import scala.reflect.ClassTag;

/**
 * Minimal hand-rolled stand-in for the Scala-emitted `Wirespec.scala` that
 * `--emit-shared` would otherwise place on the user's classpath. Exists only
 * in the kotest module's jvmTest source set so the Scala adapter's reflective
 * decoding can be exercised without applying the Gradle Scala plugin.
 *
 * The shape mirrors `examples/scala-zio/target/generated-sources/.../Wirespec.scala`
 * and the IR converter's `PackageName.convert()` output. Components use
 * Scala-stdlib types so reflective accessor lookups in `ScalaInterop` find
 * the same return types they'd find at user runtime.
 *
 * Only the subset of `GeneratorField*` variants used by the test is declared
 * here; if the test grows to cover more variants, add the matching records.
 */
public final class Wirespec {

    private Wirespec() {}

    public sealed interface GeneratorField<T>
        permits GeneratorFieldString, GeneratorFieldInteger, GeneratorFieldNumber,
                GeneratorFieldBoolean, GeneratorFieldBytes, GeneratorFieldEnum,
                GeneratorFieldUnion, GeneratorFieldArray, GeneratorFieldNullable,
                GeneratorFieldShape, GeneratorFieldDict {}

    public record GeneratorFieldString(
        Option<String> regex,
        List<Map<String, Object>> annotations
    ) implements GeneratorField<String> {}

    public record GeneratorFieldInteger(
        Option<Long> min,
        Option<Long> max,
        List<Map<String, Object>> annotations
    ) implements GeneratorField<Long> {}

    public record GeneratorFieldNumber(
        Option<Double> min,
        Option<Double> max,
        List<Map<String, Object>> annotations
    ) implements GeneratorField<Double> {}

    public record GeneratorFieldBoolean(
        List<Map<String, Object>> annotations
    ) implements GeneratorField<Boolean> {}

    public record GeneratorFieldBytes(
        List<Map<String, Object>> annotations
    ) implements GeneratorField<byte[]> {}

    public record GeneratorFieldEnum(
        List<String> values,
        List<Map<String, Object>> annotations,
        ClassTag<?> type
    ) implements GeneratorField<String> {}

    public record GeneratorFieldUnion(
        List<String> variants,
        List<Map<String, Object>> annotations,
        ClassTag<?> type
    ) implements GeneratorField<String> {}

    public record GeneratorFieldArray<T>(
        Function1<List<String>, T> generate
    ) implements GeneratorField<List<T>> {}

    public record GeneratorFieldNullable<T>(
        Function1<List<String>, T> generate
    ) implements GeneratorField<Option<T>> {}

    public record GeneratorFieldShape<T>(
        Map<String, List<Map<String, Object>>> annotations,
        Function1<List<String>, T> generate,
        ClassTag<?> type
    ) implements GeneratorField<T> {}

    public record GeneratorFieldDict<V>(
        Function1<List<String>, V> generate
    ) implements GeneratorField<Map<String, V>> {}

    public interface Generator {
        <T> T generate(List<String> path, GeneratorField<T> field);
    }
}
```

- [ ] **Step 3: Write the failing end-to-end test**

Create `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecScalaGeneratorJvmTest.kt`:

```kotlin
package community.flock.wirespec.integration.kotest

import community.flock.wirespec.scala.Wirespec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import scala.Option
import scala.collection.immutable.`List$`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cover the Scala adapter against a Java fixture (in jvmTest/java) that
 * stands in for the user's emit-shared `Wirespec.scala`. Confirms:
 *   1. `kotestWirespecScalaGenerator(...)` returns something castable to the
 *      Scala `Wirespec.Generator` declared by the fixture.
 *   2. The reflective decoding routes a `GeneratorFieldString` through the
 *      commonMain algorithm.
 *   3. `GeneratorFieldNullable<T>` returns a `scala.Option<T>`, not a bare
 *      Kotlin `T?`.
 *
 * Wider variant coverage is left for an integration test against
 * `examples/scala-zio` (follow-up), where the runtime is real Scala-emitted
 * code rather than a Java fixture.
 */
class KotestWirespecScalaGeneratorJvmTest {

    @Test
    fun `factory returns a value assignable to the Scala Wirespec_Generator`() {
        val gen = kotestWirespecScalaGenerator(seed = 1L) as Wirespec.Generator
        assertNotNull(gen)
    }

    @Test
    fun `adapter routes Scala Wirespec_GeneratorFieldString through the algorithm`() {
        val gen = kotestWirespecScalaGenerator(seed = 0L) {
            register("orderId") { Arb.constant("ORD-SCALA") }
        } as Wirespec.Generator

        val annotations = `List$`.`MODULE$`.empty<scala.collection.immutable.Map<String, Any>>()
            .prepended(
                scalaMapOf(
                    "name" to "Generator",
                    "parameters" to scalaMapOf("default" to "orderId"),
                ),
            )

        val v: String = gen.generate(
            scalaListOf("x"),
            Wirespec.GeneratorFieldString(Option.empty(), annotations),
        )
        assertEquals("ORD-SCALA", v)
    }

    @Test
    fun `adapter wraps GeneratorFieldNullable result in scala_Option`() {
        val gen = kotestWirespecScalaGenerator(seed = 0L) as Wirespec.Generator

        val nullableField = Wirespec.GeneratorFieldNullable<String> { _ -> "value" }
        val v: Option<String> = gen.generate(scalaListOf("n"), nullableField)
        assertTrue(v.isDefined)
        assertEquals("value", v.get())
    }

    // --- helpers ---

    private fun scalaListOf(vararg xs: String): scala.collection.immutable.List<String> {
        var acc: scala.collection.immutable.List<String> = `List$`.`MODULE$`.empty()
        for (x in xs.reversed()) acc = acc.prepended(x)
        return acc
    }

    private fun scalaMapOf(vararg entries: Pair<String, Any>): scala.collection.immutable.Map<String, Any> {
        var acc: scala.collection.immutable.Map<String, Any> =
            scala.collection.immutable.`Map$`.`MODULE$`.empty()
        for ((k, v) in entries) acc = acc.updated(k, v) as scala.collection.immutable.Map<String, Any>
        return acc
    }
}
```

- [ ] **Step 4: Run the test and verify it fails**

Run: `./gradlew :src:integration:kotest:jvmTest --tests "community.flock.wirespec.integration.kotest.KotestWirespecScalaGeneratorJvmTest"`
Expected: tests run but fail — first pass-through hits the `TODO("ScalaInterop.dispatch — implemented in Task 7")` from Task 5's stub. Stack trace mentions `NotImplementedError`.

(If the build also fails because `scala3-library_3` isn't on the classpath, fix the Gradle config first — likely a version-coordinate typo or missing `mavenCentral()` repo.)

---

## Task 7: Scala adapter — full reflective dispatch

**Files:**
- Modify: `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/ScalaInterop.kt`

Implements the reflective Scala ↔ Kotlin conversions so the adapter can decode `GeneratorField*` instances and re-wrap results in Scala types. All Scala-stdlib lookups are cached in `lazy` properties.

- [ ] **Step 1: Replace `ScalaInterop.kt` with the full implementation**

```kotlin
package community.flock.wirespec.integration.kotest

import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.typeOf

/**
 * Reflective Scala ↔ Kotlin conversions used by [WirespecScalaGeneratorAdapter].
 *
 * All Scala-stdlib `Class.forName` lookups happen once via `lazy`; subsequent
 * calls pay only `Method.invoke` cost. The adapter has no compile-time
 * dependency on Scala — every Scala type is named by string FQN here.
 */
internal object ScalaInterop {

    // --- Scala-stdlib reflective handles, all lazy --------------------------

    private val cl: ClassLoader by lazy {
        Thread.currentThread().contextClassLoader ?: javaClass.classLoader
    }

    private val optionModule: Any by lazy {
        cl.loadClass("scala.Option\$").getField("MODULE\$").get(null)
    }
    private val optionApply: Method by lazy {
        optionModule.javaClass.getMethod("apply", Any::class.java)
    }
    private val optionEmpty: Method by lazy {
        optionModule.javaClass.getMethod("empty")
    }

    private val listModule: Any by lazy {
        cl.loadClass("scala.collection.immutable.List\$").getField("MODULE\$").get(null)
    }
    private val listEmpty: Method by lazy {
        listModule.javaClass.getMethod("empty")
    }

    private val convertersModule: Any by lazy {
        cl.loadClass("scala.jdk.javaapi.CollectionConverters\$").getField("MODULE\$").get(null)
    }
    private val convertersAsJava: Method by lazy {
        convertersModule.javaClass.getMethod("asJava", cl.loadClass("scala.collection.Iterable"))
    }
    private val convertersAsScala: Method by lazy {
        convertersModule.javaClass.getMethod("asScala", java.lang.Iterable::class.java)
    }

    private val classTagAnyMethod: Method by lazy {
        val module = cl.loadClass("scala.reflect.ClassTag\$").getField("MODULE\$").get(null)
        module.javaClass.getMethod("Any")
    }

    // --- Public dispatch ----------------------------------------------------

    fun dispatch(inner: KotestGenerator, method: Method, args: Array<Any?>): Any? {
        check(method.name == "generate") {
            "Scala adapter received unexpected method call: ${method.name}"
        }
        val scalaPath = args[0]!!
        val scalaField = args[1]!!
        val kotlinPath = scalaListToKotlin(scalaPath)
        val kotestField = scalaFieldToKotest(scalaField)
        val resultKotlin = inner.generate(kotlinPath, kotestField)
        return adaptResultForScala(resultKotlin, scalaField)
    }

    // --- Conversions --------------------------------------------------------

    /** scala.collection.immutable.List<String> → kotlin List<String>. */
    @Suppress("UNCHECKED_CAST")
    fun scalaListToKotlin(scalaList: Any): List<String> {
        val asJavaIterable = convertersAsJava.invoke(convertersModule, scalaList) as Iterable<*>
        return asJavaIterable.map { it as String }
    }

    /** kotlin List<X> → scala.collection.immutable.List<X>. */
    private fun kotlinListToScala(xs: List<*>): Any {
        // Build a java.util.List, then ask CollectionConverters to turn it into
        // a scala Seq, then `.toList`.
        val javaList = ArrayList<Any?>(xs)
        val scalaIterable = convertersAsScala.invoke(convertersModule, javaList)
        // scalaIterable is a scala.collection.Iterable; call `.toList()`.
        val toList = scalaIterable.javaClass.getMethod("toList")
        return toList.invoke(scalaIterable)
    }

    /** kotlin Map<String, V> → scala.collection.immutable.Map<String, V>. */
    private fun kotlinMapToScala(m: Map<*, *>): Any {
        val javaMap = HashMap<Any?, Any?>(m)
        val scalaIterableLike = convertersAsScala.invoke(convertersModule, javaMap)
        val toMap = scalaIterableLike.javaClass.getMethod("toMap", cl.loadClass("scala.\$less\$colon\$less"))
        // scala.<:< has a `refl()` factory.
        val lessColonLessModule = cl.loadClass("scala.\$less\$colon\$less\$").getField("MODULE\$").get(null)
        val refl = lessColonLessModule.javaClass.getMethod("refl").invoke(lessColonLessModule)
        return toMap.invoke(scalaIterableLike, refl)
    }

    /** scala.Option<T> → kotlin T?, or returns the value if it isn't an Option. */
    private fun scalaOptionToNullable(option: Any?): Any? {
        if (option == null) return null
        val isEmpty = option.javaClass.getMethod("isEmpty").invoke(option) as Boolean
        if (isEmpty) return null
        return option.javaClass.getMethod("get").invoke(option)
    }

    /** kotlin T? → scala.Option<T>. */
    private fun nullableToScalaOption(value: Any?): Any =
        if (value == null) optionEmpty.invoke(optionModule) else optionApply.invoke(optionModule, value)

    /** kotlin (List<String>) -> R → scala.Function1<List<String>, R>. */
    private fun kotlinFnToScalaFunction1(fn: (List<String>) -> Any?): Any {
        val function1Class = cl.loadClass("scala.Function1")
        val handler = java.lang.reflect.InvocationHandler { _, m, a ->
            when (m.name) {
                "apply" -> {
                    val scalaArg = a[0]!!
                    val kotlinArg = scalaListToKotlin(scalaArg)
                    fn(kotlinArg)
                }
                // hashCode / equals / toString round-trip
                "hashCode" -> System.identityHashCode(this)
                "equals" -> a[0] === this
                "toString" -> "kotestWirespecScalaGenerator-fn"
                else -> throw UnsupportedOperationException("Unsupported Function1 method ${m.name}")
            }
        }
        return Proxy.newProxyInstance(cl, arrayOf(function1Class), handler)
    }

    private val classTagAny: Any by lazy {
        val module = cl.loadClass("scala.reflect.ClassTag\$").getField("MODULE\$").get(null)
        classTagAnyMethod.invoke(module)
    }

    /**
     * Decode a Scala `Wirespec$GeneratorField*` case-class instance into the
     * commonMain `KotestField*` mirror by case-class accessor name. Each
     * Scala case class generates a `<componentName>()` accessor matching its
     * constructor parameter — the same convention Java records use.
     */
    @Suppress("UNCHECKED_CAST")
    fun scalaFieldToKotest(field: Any): KotestField<Any?> {
        val cls = field.javaClass
        val simple = cls.simpleName
        return when (simple) {
            "GeneratorFieldString" -> {
                val regex = scalaOptionToNullable(cls.getMethod("regex").invoke(field)) as String?
                val annotations = decodeAnnotations(cls.getMethod("annotations").invoke(field))
                KotestFieldString(regex, annotations) as KotestField<Any?>
            }
            "GeneratorFieldInteger" -> {
                val min = scalaOptionToNullable(cls.getMethod("min").invoke(field)) as Long?
                val max = scalaOptionToNullable(cls.getMethod("max").invoke(field)) as Long?
                val annotations = decodeAnnotations(cls.getMethod("annotations").invoke(field))
                KotestFieldInteger(min, max, annotations) as KotestField<Any?>
            }
            "GeneratorFieldNumber" -> {
                val min = scalaOptionToNullable(cls.getMethod("min").invoke(field)) as Double?
                val max = scalaOptionToNullable(cls.getMethod("max").invoke(field)) as Double?
                val annotations = decodeAnnotations(cls.getMethod("annotations").invoke(field))
                KotestFieldNumber(min, max, annotations) as KotestField<Any?>
            }
            "GeneratorFieldBoolean" -> KotestFieldBoolean(decodeAnnotations(cls.getMethod("annotations").invoke(field))) as KotestField<Any?>
            "GeneratorFieldBytes" -> KotestFieldBytes(decodeAnnotations(cls.getMethod("annotations").invoke(field))) as KotestField<Any?>
            "GeneratorFieldEnum" -> {
                val values = scalaListToKotlin(cls.getMethod("values").invoke(field))
                val annotations = decodeAnnotations(cls.getMethod("annotations").invoke(field))
                KotestFieldEnum(values, annotations, typeOf<Any>()) as KotestField<Any?>
            }
            "GeneratorFieldUnion" -> {
                val variants = scalaListToKotlin(cls.getMethod("variants").invoke(field))
                val annotations = decodeAnnotations(cls.getMethod("annotations").invoke(field))
                KotestFieldUnion(variants, annotations, typeOf<Any>()) as KotestField<Any?>
            }
            "GeneratorFieldArray" -> {
                val scalaFn = cls.getMethod("generate").invoke(field)!!
                KotestFieldArray<Any> { p -> invokeScalaFunction1(scalaFn, p) } as KotestField<Any?>
            }
            "GeneratorFieldNullable" -> {
                val scalaFn = cls.getMethod("generate").invoke(field)!!
                KotestFieldNullable<Any> { p -> invokeScalaFunction1(scalaFn, p) } as KotestField<Any?>
            }
            "GeneratorFieldShape" -> {
                val scalaFn = cls.getMethod("generate").invoke(field)!!
                val scalaAnnotations = cls.getMethod("annotations").invoke(field)
                val annotations = decodeShapeAnnotations(scalaAnnotations)
                KotestFieldShape<Any>(annotations, { p -> invokeScalaFunction1(scalaFn, p) }, typeOf<Any>()) as KotestField<Any?>
            }
            "GeneratorFieldDict" -> {
                val scalaFn = cls.getMethod("generate").invoke(field)!!
                KotestFieldDict<Any> { p -> invokeScalaFunction1(scalaFn, p) } as KotestField<Any?>
            }
            else -> error("Unrecognised Scala GeneratorField: $simple")
        }
    }

    /**
     * Re-wrap the kotest result in the shape expected by the originating
     * Scala field type:
     *   - GeneratorFieldNullable   → result wrapped in scala.Option
     *   - GeneratorFieldArray      → result (a kotlin List) wrapped in scala.collection.immutable.List
     *   - GeneratorFieldDict       → result (a kotlin Map) wrapped in scala.collection.immutable.Map
     *   - everything else          → returned as-is (primitives, Strings, byte[])
     */
    fun adaptResultForScala(result: Any?, originalField: Any): Any? = when (originalField.javaClass.simpleName) {
        "GeneratorFieldNullable" -> nullableToScalaOption(result)
        "GeneratorFieldArray" -> kotlinListToScala(result as List<*>)
        "GeneratorFieldDict" -> kotlinMapToScala(result as Map<*, *>)
        else -> result
    }

    // --- Helpers ------------------------------------------------------------

    /** scala.Function1.apply(kotlinList) — wraps the kotlin list back as Scala first. */
    private fun invokeScalaFunction1(scalaFn: Any, kotlinPath: List<String>): Any {
        val scalaArg = kotlinListToScala(kotlinPath)
        val applyMethod = scalaFn.javaClass.getMethod("apply", Any::class.java)
        return applyMethod.invoke(scalaFn, scalaArg)
    }

    /** Convert `scala.collection.immutable.List<scala.collection.immutable.Map<String,Any>>` → kotlin equivalent. */
    @Suppress("UNCHECKED_CAST")
    private fun decodeAnnotations(scalaAnnotations: Any?): List<Map<String, Any>> {
        if (scalaAnnotations == null) return emptyList()
        val asJavaIterable = convertersAsJava.invoke(convertersModule, scalaAnnotations) as Iterable<*>
        return asJavaIterable.map { scalaMap -> decodeScalaMap(scalaMap!!) }
    }

    /**
     * Convert Shape annotations (`Map[String, List[Map[String,Any]]]`) — a
     * Map whose values are themselves Lists of Maps. Both layers go through
     * CollectionConverters; nested Map values are recursively converted by
     * `decodeAnnotations`.
     */
    @Suppress("UNCHECKED_CAST")
    private fun decodeShapeAnnotations(scalaShape: Any?): Map<String, List<Map<String, Any>>> {
        if (scalaShape == null) return emptyMap()
        val asJavaMap = convertersAsJava.invoke(convertersModule, scalaShape) as Map<String, Any>
        return asJavaMap.mapValues { (_, scalaList) -> decodeAnnotations(scalaList) }
    }

    /** scala.collection.immutable.Map<String,Any> → kotlin Map<String,Any>. Nested Maps are recursively converted. */
    @Suppress("UNCHECKED_CAST")
    private fun decodeScalaMap(scalaMap: Any): Map<String, Any> {
        val asJava = convertersAsJava.invoke(convertersModule, scalaMap) as Map<String, Any>
        return asJava.mapValues { (_, v) ->
            val cls = v.javaClass.name
            if (cls.startsWith("scala.collection.immutable.Map")) decodeScalaMap(v) else v
        }
    }
}
```

- [ ] **Step 2: Run the Scala adapter test and verify it passes**

Run: `./gradlew :src:integration:kotest:jvmTest --tests "community.flock.wirespec.integration.kotest.KotestWirespecScalaGeneratorJvmTest"`
Expected: BUILD SUCCESSFUL, all 3 tests in `KotestWirespecScalaGeneratorJvmTest` pass.

- [ ] **Step 3: Run the full kotest jvmTest to confirm nothing else regressed**

Run: `./gradlew :src:integration:kotest:jvmTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add src/integration/kotest/build.gradle.kts \
        src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/ScalaInterop.kt \
        src/integration/kotest/src/jvmTest/java/community/flock/wirespec/scala/Wirespec.java \
        src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecScalaGeneratorJvmTest.kt
git commit -m "feat(integration/kotest): full ScalaInterop reflective decoding

Decodes Scala Wirespec\$GeneratorField* case-class instances into
KotestField* by accessor reflection, re-wraps Nullable results in
scala.Option, Array results in scala.collection.immutable.List, and
Dict results in scala.collection.immutable.Map. All Scala-stdlib
lookups cached in lazy properties.

Test fixture (jvmTest/java/community/flock/wirespec/scala/Wirespec.java)
stands in for emit-shared output using Scala-stdlib types, so the
adapter is exercised without applying the Gradle Scala plugin.

Wider variant coverage against real Scala-emitted code is left for a
follow-up integration test in examples/scala-zio."
```

---

## Task 8: README — document the three siblings

**Files:**
- Modify: `src/integration/kotest/README.md`

The existing README walks through `kotestWirespecGenerator(...)`. Update the function name and add new sections for Java and Scala.

- [ ] **Step 1: Replace the README**

Open `src/integration/kotest/README.md`. Three localised edits — keep everything else.

(a) **Replace every `kotestWirespecGenerator(` with `kotestWirespecKotlinGenerator(`** in the Kotlin code blocks (5 occurrences).

(b) **Replace the `Basic usage` section heading** (currently `## Basic usage`) with `## Basic usage — Kotlin-emitted code`.

(c) **Insert two new sections** between `## Basic usage — Kotlin-emitted code` (closing ```` ``` ````) and `## Default `@Generator(...)` registrations`:

````markdown
## Java-emitted code

The Java emitter produces `*Generator.java` factories that take a
`community.flock.wirespec.java.Wirespec.Generator`. Use the Java sibling
factory:

```kotlin
import community.flock.wirespec.integration.kotest.kotestWirespecJavaGenerator
import community.flock.wirespec.java.Wirespec
import com.example.generated.MemberGenerator

val gen: Wirespec.Generator = kotestWirespecJavaGenerator(seed = 1L) {
    register("orderId") { Arb.uuid().map(java.util.UUID::toString) }
}
val member = MemberGenerator.generate(gen, java.util.List.of())
```

Same DSL, default arb catalog, and `@Generator` / `@Seed` semantics as the
Kotlin sibling. The two differences are JVM-flavoured:

- `GeneratorFieldString.regex`, `GeneratorFieldInteger.min`/`max`, etc. carry
  `java.util.Optional<X>` instead of Kotlin's `X?` — the adapter handles the
  `Optional.empty()` ↔ `null` translation.
- `GeneratorFieldNullable<T>` returns `java.util.Optional<T>` (Java semantics)
  rather than the bare `T?` the commonMain algorithm produces.

## Scala-emitted code

The Scala emitter produces `*Generator.scala` factories that take a
`community.flock.wirespec.scala.Wirespec.Generator`. Use the Scala sibling
factory and cast at the call site — the kotest module has zero compile-time
Scala dependency, so the factory's static return type is `Any`:

```kotlin
import community.flock.wirespec.integration.kotest.kotestWirespecScalaGenerator
import community.flock.wirespec.scala.Wirespec  // from your --emit-shared output
import com.example.generated.generator.MemberGenerator

val gen: Wirespec.Generator =
    kotestWirespecScalaGenerator(seed = 1L) {
        register("orderId") { Arb.uuid().map(java.util.UUID::toString) }
    } as Wirespec.Generator

val member = MemberGenerator.generate(gen, scala.collection.immutable.List.empty())
```

**Requirement:** the user's codegen MUST run with `--emit-shared` so the
generated `Wirespec.scala` (which declares `Wirespec.Generator`) lands on the
test classpath. If it's missing, the first call raises:

> *Scala-emitted Wirespec.scala not found on classpath. Run your codegen with
> --emit-shared and make sure the generated source set is on the test
> compile/runtime classpath.*

Internally the Scala adapter is a `java.lang.reflect.Proxy` that resolves
`Wirespec.Generator` reflectively at construction time. Reflective Scala ↔
Kotlin conversions live in `ScalaInterop.kt`.
````

- [ ] **Step 2: Verify the README renders cleanly**

Run: `./gradlew :src:integration:kotest:build`
Expected: BUILD SUCCESSFUL (no doc tooling will fail; this just sanity-checks the module still builds with all changes in place).

- [ ] **Step 3: Commit**

```bash
git add src/integration/kotest/README.md
git commit -m "docs(integration/kotest): document Java and Scala sibling factories

- Rename kotestWirespecGenerator -> kotestWirespecKotlinGenerator in examples
- Add Java-emitted code section
- Add Scala-emitted code section with --emit-shared caveat and call-site cast"
```

---

## Self-review checklist

Spec coverage:
- ✅ Generator types added to `Wirespec.java` — Task 1.
- ✅ Three sibling factories with symmetric naming — Tasks 2 (Kotlin rename), 3+4 (Java), 5+6+7 (Scala).
- ✅ `KotestField*` mirror reused; no commonMain changes — verified by all adapter implementations.
- ✅ `GeneratorFieldNullable<T>` `Optional` round-trip for Java — Task 3 test, Task 4 implementation.
- ✅ Scala dynamic-proxy with reflective Scala-stdlib interop — Tasks 5, 6, 7.
- ✅ Class-not-found error message for missing Scala runtime — Task 5 implementation, surfaced in README — Task 8.
- ✅ Tests for all three adapters — Tasks 3 (Java), 6 (Scala), and the renamed-in-place Kotlin test — Task 2.
- ✅ README updated — Task 8.
- ✅ No new Gradle subprojects (per spec); no `js` changes — verified.

Red flags:
- No "TBD"/"TODO" placeholders in any task body except the **deliberate** `TODO(...)` in Task 5's `ScalaInterop` stub, which Task 7 replaces. Each task ships compilable code.
- Type/method names cross-checked: `WirespecKotlinGeneratorAdapter` (Task 2) vs `WirespecJavaGeneratorAdapter` (Task 4) vs `WirespecScalaGeneratorAdapter` (Task 5) — symmetric and never referenced inconsistently. `ScalaInterop.dispatch` signature in Task 5 stub matches Task 7's full implementation.

---

## Follow-ups (out of scope here)

- Integration test for the Scala adapter against real emit-shared output in `examples/scala-zio` — proves the Java-fixture-based unit test stays representative.
- Cover the remaining `GeneratorField*` variants in the Scala unit test (currently only String + Nullable are exercised end-to-end).
- A property-test demo in an existing `examples/maven-spring-*` (Java) module mirroring `examples/gradle-spring-boot`'s Kotlin demo.
