# Kotest Generator Overrides Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the existing `@Generator(name)` lookup on `KotestWirespecGeneratorBuilder` with two scoped override mechanisms — `registerField<Parent>(name)` (by parent type + field name) and `registerPath(...)` (by absolute path with `*` wildcard) — and auto-wrap drawn values when the target field is a Refined wrapper.

**Architecture:** Add a small `OverrideRegistry` (path patterns + field-key map) to the builder, thread it into the existing `KotestWirespecGenerator`, and slot two lookup hooks into the precedence order *after* `@Seed` but *before* the default leaf. Refined auto-wrap crosses the commonMain↔jvmMain boundary via a `RefinedWrapper` SPI (default = identity; JVM installs a reflective wrapper). Delete the obsolete `register(name)` / `DEFAULT_ARBS` / `namedGeneratorOrNull` machinery in the same pass.

**Tech Stack:** Kotlin Multiplatform (commonMain + jvmMain + jsMain), kotest-property, `kotlin.reflect` (JVM-only for Refined-ctor lookup), JUnit-style `kotlin.test` runners.

---

## File Structure

**commonMain (kotest module):**
- `KotestField.kt` — unchanged
- `KotestWirespecGenerator.kt` — modify: strip `register(name)`, `namedArbs`, `@Generator` lookup; replace `shapeDepth` with `parentStack`; add path & field lookup hooks; thread `RefinedWrapper` through the constructor
- `KotestOverrides.kt` — **create**: `PathSegment`, `PathPattern`, `FieldKey`, `OverrideRegistry`, `RefinedWrapper`, `IdentityRefinedWrapper`
- `DefaultArbs.kt` — **delete**

**jvmMain (kotest module):**
- `KotestWirespecKotlinGenerator.kt` — modify: pass `JvmRefinedWrapper` to the inner generator
- `KotestWirespecJavaGenerator.kt` — modify: same
- `KotestWirespecScalaGenerator.kt` — modify: same
- `KotestBuilderJvm.kt` — **create**: reified `registerField<Parent>` extensions; `JvmRefinedWrapper` object + Refined-ctor cache

**jsMain (kotest module):**
- `KotestWirespecGeneratorJs.kt` — modify: drop the `registrations` parameter (no more `@Generator` to register against)

**commonTest:**
- `KotestWirespecGeneratorTest.kt` — modify: delete three `@Generator` cases
- `DefaultArbsTest.kt` — **delete**
- `KotestOverrideTest.kt` — **create**: path & field override behavior, precedence, error cases

**jvmTest:**
- `KotestWirespecKotlinGeneratorJvmTest.kt` — modify: delete `@Generator` case
- `KotestWirespecJavaGeneratorJvmTest.kt` — modify: delete `@Generator` case
- `KotestWirespecScalaGeneratorJvmTest.kt` — modify: delete `@Generator` case
- `KotestWirespecKotlinGeneratorOverrideJvmTest.kt` — **create**: reified `registerField<Parent>`, Refined auto-wrap, value shorthand, type-mismatch error

**jsTest:**
- `KotestWirespecGeneratorJsTest.kt` — modify: delete three `@Generator`/`registrations` cases

---

## Build commands

- Fast feedback: `./gradlew :src:integration:kotest:jvmTest`
- Common (multiplatform) tests: `./gradlew :src:integration:kotest:jvmTest :src:integration:kotest:jsNodeTest`
- Single class filter: `./gradlew :src:integration:kotest:jvmTest --tests "community.flock.wirespec.integration.kotest.KotestOverrideTest"`
- Full check: `./gradlew :src:integration:kotest:allTests`

The whole feature lives inside one Gradle module — no other modules touch it.

---

## Task 1: Strip the `@Generator` lookup machinery

This is a pure removal — it lands the codebase in a state where no `register(name)` API exists and `@Generator(...)` annotations are silently ignored. Every existing non-`@Generator` test still passes.

**Files:**
- Delete: `src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/DefaultArbs.kt`
- Delete: `src/integration/kotest/src/commonTest/kotlin/community/flock/wirespec/integration/kotest/DefaultArbsTest.kt`
- Modify: `src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGenerator.kt`
- Modify: `src/integration/kotest/src/commonTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorTest.kt`
- Modify: `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecKotlinGeneratorJvmTest.kt`
- Modify: `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecJavaGeneratorJvmTest.kt`
- Modify: `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecScalaGeneratorJvmTest.kt`
- Modify: `src/integration/kotest/src/jsMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJs.kt`
- Modify: `src/integration/kotest/src/jsTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGeneratorJsTest.kt`

- [ ] **Step 1: Delete `DefaultArbs.kt`**

```bash
git rm src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/DefaultArbs.kt
```

- [ ] **Step 2: Delete `DefaultArbsTest.kt`**

```bash
git rm src/integration/kotest/src/commonTest/kotlin/community/flock/wirespec/integration/kotest/DefaultArbsTest.kt
```

- [ ] **Step 3: Strip `register(name)`, `namedArbs`, and `@Generator` lookup from `KotestWirespecGenerator.kt`**

Replace the existing file content with the version below. Everything related to `@Generator`/`namedArbs`/`DEFAULT_ARBS` is gone; the public factory, builder class, and the generator class survive but with a leaner signature.

```kotlin
package community.flock.wirespec.integration.kotest

import community.flock.kotlinx.rgxgen.RgxGen
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.next

/**
 * `KotestGenerator` backed by Kotest [Arb]s. The IR-emitted
 * `*Generator.generate(...)` factories accept anything compatible with
 * `KotestGenerator`; use [kotestGenerator] (commonMain) or one of the JVM
 * factories (`kotestWirespecKotlinGenerator`, `kotestWirespecJavaGenerator`,
 * `kotestWirespecScalaGenerator`) to obtain one.
 *
 * `@Seed` annotations are honored for deterministic regeneration of array
 * elements from their seed value. Any `@Generator(...)` annotation that the
 * IR still emits is ignored — replace it with a scoped override (see the
 * `registerField` / `registerPath` extensions added in later tasks).
 */
fun kotestGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): KotestGenerator {
    val builder = KotestWirespecGeneratorBuilder().apply(block)
    return KotestWirespecGenerator(seed)
}

class KotestWirespecGeneratorBuilder internal constructor()

internal class KotestWirespecGenerator(
    private val baseSeed: Long,
) : KotestGenerator {

    private val pendingSeeds = ArrayDeque<PendingSeed>()

    private val captures = ArrayDeque<Capture>()

    private var shapeDepth = 0

    private data class PendingSeed(val value: String, val target: String, val pathPrefix: List<String>)

    private class Capture(val shapePath: List<String>, val fieldName: String) {
        var seed: String? = null
    }

    private fun rsFor(path: List<String>): RandomSource =
        RandomSource.seeded(baseSeed xor path.joinToString("/").hashCode().toLong())

    private inline fun <R> withShapeDepth(block: () -> R): R {
        shapeDepth++
        return try {
            block()
        } finally {
            shapeDepth--
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> generate(
        path: List<String>,
        field: KotestField<T>,
    ): T {
        captureSeedIfMatches(path, field)?.let { return it as T }

        consumePendingSeedIfMatches(path, field)?.let { return it as T }

        val annotations = field.fieldAnnotations()

        if (shapeDepth == 0 && annotations.any { it["name"] == "Seed" }) {
            path.dropLast(1).lastOrNull()?.let { candidate ->
                seedAnnotationValueFor(field, candidate)?.let { return it as T }
            }
        }

        if (shapeDepth == 0 && field is KotestFieldShape<*>) {
            seedFieldNameOf(field)?.let { seedFieldName ->
                generateSeededShape(path, field, seedFieldName)?.let { return it as T }
            }
        }

        return generateLeaf(field, path)
    }

    private fun captureSeedIfMatches(path: List<String>, field: KotestField<*>): Any? {
        val capture = captures.lastOrNull() ?: return null
        if (capture.seed != null) return null
        val expectedPrefix = capture.shapePath + capture.fieldName
        if (path.size < expectedPrefix.size || path.subList(0, expectedPrefix.size) != expectedPrefix) return null
        val rs = rsFor(path)
        return when (field) {
            is KotestFieldString -> {
                val prefix = field.regex?.let { "" } ?: (path.lastOrNull().orEmpty() + "-")
                val regex = field.regex ?: "\\w{8}"
                val value = prefix + RgxGen.parse(regex).generate(rs.random)
                capture.seed = value
                value
            }
            is KotestFieldInteger64 -> {
                val lo = field.min ?: 0
                val hi = field.max ?: Long.MAX_VALUE
                val value = Arb.long(lo..hi).next(rs)
                capture.seed = value.toString()
                value
            }
            is KotestFieldInteger32 -> {
                val lo = field.min ?: 0
                val hi = field.max ?: Int.MAX_VALUE
                val value = Arb.int(lo..hi).next(rs)
                capture.seed = value.toString()
                value
            }
            else -> null
        }
    }

    private fun consumePendingSeedIfMatches(path: List<String>, field: KotestField<*>): Any? {
        val pending = pendingSeeds.lastOrNull() ?: return null
        if (path.size != pending.pathPrefix.size + 1) return null
        if (path.last() != pending.target) return null
        if (path.subList(0, pending.pathPrefix.size) != pending.pathPrefix) return null
        return when (field) {
            is KotestFieldString -> {
                pendingSeeds.removeLast()
                pending.value
            }
            is KotestFieldInteger64 -> {
                pendingSeeds.removeLast()
                pending.value.toLong()
            }
            is KotestFieldInteger32 -> {
                pendingSeeds.removeLast()
                pending.value.toInt()
            }
            else -> null
        }
    }

    private fun seedAnnotationValueFor(field: KotestField<*>, candidate: String): Any? = when (field) {
        is KotestFieldString -> candidate
        is KotestFieldInteger64 -> candidate.toLongOrNull()
        is KotestFieldInteger32 -> candidate.toIntOrNull()
        else -> null
    }

    private fun generateSeededShape(
        path: List<String>,
        field: KotestFieldShape<*>,
        seedFieldName: String,
    ): Any? {
        val isArrayContext = path.lastOrNull()?.toIntOrNull() != null
        if (isArrayContext && captures.isEmpty()) {
            val capture = Capture(path, seedFieldName)
            val seed = withFrame(captures, capture) {
                withShapeDepth { field.generate(path) }
                capture.seed ?: error("Failed to capture @Seed value at $path for field $seedFieldName")
            }
            return field.generate(listOf(seed))
        }

        val candidate = path.dropLast(1).lastOrNull() ?: return null
        return withFrame(pendingSeeds, PendingSeed(candidate, seedFieldName, path)) {
            withShapeDepth { field.generate(path) }
        }
    }

    private fun seedFieldNameOf(field: KotestFieldShape<*>): String? = field.annotations.entries
        .firstOrNull { (_, anns) -> anns.any { it["name"] == "Seed" } }
        ?.key

    @Suppress("UNCHECKED_CAST")
    private fun <T> generateLeaf(field: KotestField<T>, path: List<String>): T {
        val rs = rsFor(path)
        return when (field) {
            is KotestFieldString -> {
                val prefix = field.regex?.let { "" } ?: (path.lastOrNull().orEmpty() + "-")
                val regex = field.regex ?: "\\w{8}"
                (prefix + RgxGen.parse(regex).generate(rs.random)) as T
            }
            is KotestFieldInteger64 -> {
                val lo = field.min ?: Long.MIN_VALUE
                val hi = field.max ?: Long.MAX_VALUE
                Arb.long(lo..hi).next(rs) as T
            }
            is KotestFieldInteger32 -> {
                val lo = field.min ?: Int.MIN_VALUE
                val hi = field.max ?: Int.MAX_VALUE
                Arb.int(lo..hi).next(rs) as T
            }
            is KotestFieldNumber64 -> {
                val lo = field.min ?: -1e6
                val hi = field.max ?: 1e6
                Arb.double(lo, hi).next(rs) as T
            }
            is KotestFieldNumber32 -> {
                val lo = field.min ?: -1e6f
                val hi = field.max ?: 1e6f
                Arb.float(lo, hi).next(rs) as T
            }
            is KotestFieldBoolean -> Arb.boolean().next(rs) as T
            is KotestFieldBytes -> Arb.byteArray(Arb.int(0..16), Arb.byte()).next(rs) as T
            is KotestFieldEnum -> Arb.element(field.values).next(rs) as T
            is KotestFieldUnion -> Arb.element(field.variants).next(rs) as T
            is KotestFieldArray<*> -> {
                val size = Arb.int(1..10).next(rs)
                (0 until size).map { i -> field.generate(path + "$i") } as T
            }
            is KotestFieldNullable<*> -> field.generate(path) as T
            is KotestFieldShape<*> -> field.generate(path) as T
            is KotestFieldDict<*> -> mapOf("a" to field.generate(path + "a")) as T
        }
    }

    private fun KotestField<*>.fieldAnnotations(): List<Map<String, Any>> = when (this) {
        is KotestFieldString -> annotations
        is KotestFieldInteger64 -> annotations
        is KotestFieldInteger32 -> annotations
        is KotestFieldNumber64 -> annotations
        is KotestFieldNumber32 -> annotations
        is KotestFieldBoolean -> annotations
        is KotestFieldBytes -> annotations
        is KotestFieldEnum -> annotations
        is KotestFieldUnion -> annotations
        else -> emptyList()
    }
}

private inline fun <F, R> withFrame(stack: ArrayDeque<F>, frame: F, block: () -> R): R {
    val mark = stack.size
    stack.addLast(frame)
    return try {
        block()
    } finally {
        while (stack.size > mark) stack.removeLast()
    }
}
```

- [ ] **Step 4: Delete `@Generator` cases from `KotestWirespecGeneratorTest.kt`**

Remove these three tests verbatim from the file (and the now-unused `Arb`/`constant` imports if they become orphaned — leave imports if other tests still need them):

```
// Delete: Generator annotation routes to a registered Arb
// Delete: Generator annotation lookup is case-insensitive
// Delete: unknown Generator name throws a clear error
```

After deletion, the file's `@Generator` section header comment block (`// ---------- Named generators (@Generator dispatch) ----------`) goes too.

- [ ] **Step 5: Delete `@Generator` case from `KotestWirespecKotlinGeneratorJvmTest.kt`**

Remove the test `adapter routes Wirespec_GeneratorFieldString through the algorithm` (lines ~29–42). Drop the now-unused `import io.kotest.property.Arb` and `import io.kotest.property.arbitrary.constant`.

- [ ] **Step 6: Delete `@Generator` case from `KotestWirespecJavaGeneratorJvmTest.kt`**

Remove the test `adapter routes Java Wirespec_GeneratorFieldString through the algorithm` (lines ~33–45). Drop the now-unused `Arb` / `constant` imports.

- [ ] **Step 7: Delete `@Generator` case from `KotestWirespecScalaGeneratorJvmTest.kt`**

Remove the test `adapter routes Scala Wirespec_GeneratorFieldString through the algorithm` (lines ~35–53). Drop the now-unused `Arb` / `constant` imports.

- [ ] **Step 8: Drop `registrations` from `KotestWirespecGeneratorJs.kt`**

Replace the `kotestWirespecGeneratorJs` factory with the no-registrations version (the inner `kotestGenerator(seed)` no longer accepts a name registry):

```kotlin
fun kotestWirespecGeneratorJs(
    seed: Int,
): KotestWirespecGeneratorJs {
    val inner = kotestGenerator(seed.toLong())
    return KotestWirespecGeneratorJs(inner)
}
```

Remove the now-unused imports (`Arb`, `arbitrary.int`, `arbitrary.map`). Keep the class body, `jsToKotestField`, `jsAnnotationsToKotlin`, etc. — they all still work.

- [ ] **Step 9: Strip the JS test for registrations**

In `KotestWirespecGeneratorJsTest.kt`, delete:
- `dynamic registrations object routes named generators through user functions`
- `registry name match is case-insensitive`

Update `kotestWirespecGeneratorJs returns a working generator with no registrations` to drop the literal text "with no registrations" from its name (the registrations param no longer exists at all):

```kotlin
@Test
fun `kotestWirespecGeneratorJs returns a working generator`() {
    val gen = kotestWirespecGeneratorJs(seed = 1)
    val field: dynamic = js("({kind: 'string', regex: undefined, annotations: []})")
    val value = gen.generate(arrayOf("a"), field) as String
    assertNotNull(value)
    assertTrue(value.isNotEmpty(), "expected non-empty string, got '$value'")
}
```

- [ ] **Step 10: Run the full test suite to verify everything is green**

```bash
./gradlew :src:integration:kotest:allTests
```

Expected: all tests pass. The `@Generator`-related tests are gone; everything else still works because the only code path we removed was annotation-driven name lookup.

- [ ] **Step 11: Commit**

```bash
git add -A src/integration/kotest
git commit -m "refactor(integration/kotest): strip @Generator name-lookup machinery

The new scoped overrides (added in subsequent commits) fully subsume the
register(name) / @Generator(\"name\") mechanism. Delete the lookup, the
DEFAULT_ARBS catalog, and the JS registrations parameter ahead of the new
override implementation.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Add `OverrideRegistry` + `RefinedWrapper` SPI

Create the override storage and the auto-wrap SPI as pure data types in commonMain. No wiring into the generator yet — Task 4 does that.

**Files:**
- Create: `src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/KotestOverrides.kt`
- Test: `src/integration/kotest/src/commonTest/kotlin/community/flock/wirespec/integration/kotest/KotestOverrideRegistryTest.kt` (new — narrowly tests the data structures; behavioral end-to-end tests come in Task 5/6)

- [ ] **Step 1: Write failing tests for `PathPattern` and `OverrideRegistry`**

Create `src/integration/kotest/src/commonTest/kotlin/community/flock/wirespec/integration/kotest/KotestOverrideRegistryTest.kt`:

```kotlin
package community.flock.wirespec.integration.kotest

import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KotestOverrideRegistryTest {

    @Test
    fun `PathPattern of literals matches the exact path only`() {
        val pat = PathPattern.compile(arrayOf("users", "0", "id"))
        assertEquals(true, pat.matches(listOf("users", "0", "id")))
        assertEquals(false, pat.matches(listOf("users", "1", "id")))
        assertEquals(false, pat.matches(listOf("users", "0")))
        assertEquals(false, pat.matches(listOf("users", "0", "id", "x")))
    }

    @Test
    fun `PathPattern wildcard matches any single segment`() {
        val pat = PathPattern.compile(arrayOf("users", "*", "id"))
        assertEquals(true, pat.matches(listOf("users", "0", "id")))
        assertEquals(true, pat.matches(listOf("users", "42", "id")))
        assertEquals(true, pat.matches(listOf("users", "anything", "id")))
        assertEquals(false, pat.matches(listOf("users", "0", "name")))
        assertEquals(false, pat.matches(listOf("orders", "0", "id")))
    }

    @Test
    fun `PathPattern specificity counts literals`() {
        val literal = PathPattern.compile(arrayOf("users", "0", "id"))
        val wild = PathPattern.compile(arrayOf("users", "*", "id"))
        val allWild = PathPattern.compile(arrayOf("*", "*", "*"))
        assertEquals(3, literal.specificity)
        assertEquals(2, wild.specificity)
        assertEquals(0, allWild.specificity)
    }

    @Test
    fun `OverrideRegistry returns the most specific path factory`() {
        val registry = OverrideRegistry()
        registry.addPath(arrayOf("users", "*", "id")) { Arb.constant("WILD") }
        registry.addPath(arrayOf("users", "0", "id")) { Arb.constant("EXACT") }

        val factory = registry.findPath(listOf("users", "0", "id"))
        assertNotNull(factory)
        // We can't easily compare lambdas — call the factory to verify which one was returned.
        assertEquals("EXACT", factory().sample(io.kotest.property.RandomSource.default()).value)
    }

    @Test
    fun `OverrideRegistry returns wildcard match when no literal exists`() {
        val registry = OverrideRegistry()
        registry.addPath(arrayOf("users", "*", "id")) { Arb.constant("WILD") }

        val factory = registry.findPath(listOf("users", "42", "id"))
        assertNotNull(factory)
        assertEquals("WILD", factory().sample(io.kotest.property.RandomSource.default()).value)
    }

    @Test
    fun `OverrideRegistry returns null when no pattern matches`() {
        val registry = OverrideRegistry()
        registry.addPath(arrayOf("users", "*", "id")) { Arb.constant("WILD") }
        assertNull(registry.findPath(listOf("orders", "0", "total")))
    }

    @Test
    fun `OverrideRegistry rejects two equally-specific patterns matching same path`() {
        val registry = OverrideRegistry()
        registry.addPath(arrayOf("users", "*", "id")) { Arb.constant("A") }
        registry.addPath(arrayOf("*", "0", "id")) { Arb.constant("B") }

        val ex = assertFailsWith<IllegalStateException> {
            registry.findPath(listOf("users", "0", "id"))
        }
        assertEquals(true, ex.message!!.contains("Ambiguous"))
    }

    @Test
    fun `OverrideRegistry stores and retrieves field overrides by FieldKey`() {
        val registry = OverrideRegistry()
        val key = FieldKey("com.example.User", "email")
        registry.addField(key) { Arb.constant("a@b.com") }
        val factory = registry.findField(key)
        assertNotNull(factory)
        assertEquals("a@b.com", factory().sample(io.kotest.property.RandomSource.default()).value)
    }

    @Test
    fun `OverrideRegistry rejects duplicate FieldKey registration`() {
        val registry = OverrideRegistry()
        val key = FieldKey("com.example.User", "email")
        registry.addField(key) { Arb.constant("a@b.com") }
        val ex = assertFailsWith<IllegalStateException> {
            registry.addField(key) { Arb.constant("c@d.com") }
        }
        assertEquals(true, ex.message!!.contains("already registered"))
    }

    @Test
    fun `IdentityRefinedWrapper passes the drawn value through unchanged`() {
        val drawn = "hello"
        val out = IdentityRefinedWrapper.wrap(drawn, KotestFieldString(regex = null, annotations = emptyList()), listOf("a"))
        assertEquals(drawn, out)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :src:integration:kotest:jvmTest --tests "community.flock.wirespec.integration.kotest.KotestOverrideRegistryTest"
```

Expected: compilation failure — `PathPattern`, `OverrideRegistry`, `FieldKey`, `IdentityRefinedWrapper` don't exist yet.

- [ ] **Step 3: Implement `KotestOverrides.kt`**

Create `src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/KotestOverrides.kt`:

```kotlin
package community.flock.wirespec.integration.kotest

import io.kotest.property.Arb

internal sealed interface PathSegment {
    data class Literal(val value: String) : PathSegment
    data object Wildcard : PathSegment
}

internal data class PathPattern(val segments: List<PathSegment>) {

    val specificity: Int = segments.count { it is PathSegment.Literal }

    fun matches(path: List<String>): Boolean {
        if (path.size != segments.size) return false
        for (i in segments.indices) {
            when (val seg = segments[i]) {
                is PathSegment.Literal -> if (seg.value != path[i]) return false
                PathSegment.Wildcard -> Unit
            }
        }
        return true
    }

    override fun toString(): String = segments.joinToString("/") {
        when (it) {
            is PathSegment.Literal -> it.value
            PathSegment.Wildcard -> "*"
        }
    }

    companion object {
        fun compile(segments: Array<out String>): PathPattern = PathPattern(
            segments.map { if (it == "*") PathSegment.Wildcard else PathSegment.Literal(it) },
        )
    }
}

internal data class FieldKey(val parentTypeName: String, val fieldName: String)

internal class OverrideRegistry {

    private val pathOverrides: MutableList<Pair<PathPattern, () -> Arb<*>>> = mutableListOf()
    private val fieldOverrides: MutableMap<FieldKey, () -> Arb<*>> = mutableMapOf()

    fun addPath(segments: Array<out String>, factory: () -> Arb<*>) {
        pathOverrides += PathPattern.compile(segments) to factory
    }

    fun addField(key: FieldKey, factory: () -> Arb<*>) {
        check(key !in fieldOverrides) {
            "Field override already registered for $key"
        }
        fieldOverrides[key] = factory
    }

    fun findPath(path: List<String>): (() -> Arb<*>)? {
        val matches = pathOverrides.filter { (pattern, _) -> pattern.matches(path) }
        if (matches.isEmpty()) return null
        val maxSpec = matches.maxOf { it.first.specificity }
        val best = matches.filter { it.first.specificity == maxSpec }
        if (best.size > 1) {
            error(
                "Ambiguous path overrides for ${path.joinToString("/")}: " +
                    best.joinToString(", ") { it.first.toString() },
            )
        }
        return best.single().second
    }

    fun findField(key: FieldKey): (() -> Arb<*>)? = fieldOverrides[key]
}

internal fun interface RefinedWrapper {
    fun wrap(drawn: Any?, field: KotestField<*>, path: List<String>): Any?
}

internal object IdentityRefinedWrapper : RefinedWrapper {
    override fun wrap(drawn: Any?, field: KotestField<*>, path: List<String>): Any? = drawn
}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :src:integration:kotest:jvmTest --tests "community.flock.wirespec.integration.kotest.KotestOverrideRegistryTest"
```

Expected: all 9 tests pass.

- [ ] **Step 5: Run the full kotest module tests to confirm nothing else regressed**

```bash
./gradlew :src:integration:kotest:allTests
```

Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/KotestOverrides.kt \
        src/integration/kotest/src/commonTest/kotlin/community/flock/wirespec/integration/kotest/KotestOverrideRegistryTest.kt
git commit -m "feat(integration/kotest): introduce OverrideRegistry and RefinedWrapper SPI

Pure-data types for the upcoming scoped overrides. Not wired into the
generator yet — subsequent commits add the lookup hooks and the JVM
reflective RefinedWrapper.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Add a parent-shape stack alongside `shapeDepth`

Adds a `parentStack` of `ParentFrame(typeName)` that records the type-name of every enclosing `KotestFieldShape` — used in Task 6 for `(parent type, field name)` lookup. The existing `shapeDepth` keeps its current role gating `@Seed`-on-primitive precedence; the two are independent. Pushing the parent on every shape entry while leaving `shapeDepth` unchanged preserves the `@Seed` semantics that depend on `shapeDepth == 0` not being incremented when entering a non-seeded shape via `generateLeaf`.

**Files:**
- Modify: `src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGenerator.kt`

- [ ] **Step 1: Run the full test suite to establish a green baseline**

```bash
./gradlew :src:integration:kotest:allTests
```

Expected: green (Task 2 left it green).

- [ ] **Step 2: Add `parentStack` and `withParentFrame` inside `KotestWirespecGenerator`**

Locate the line `private var shapeDepth = 0` and **add** these declarations directly below it (do not delete `shapeDepth`):

```kotlin
    private val parentStack = ArrayDeque<ParentFrame>()

    private data class ParentFrame(val typeName: String)

    private inline fun <R> withParentFrame(frame: ParentFrame, block: () -> R): R {
        parentStack.addLast(frame)
        return try {
            block()
        } finally {
            parentStack.removeLast()
        }
    }
```

- [ ] **Step 3: Push the parent frame on every shape entry in `generateLeaf`**

Replace the line:
```kotlin
            is KotestFieldShape<*> -> field.generate(path) as T
```
with:
```kotlin
            is KotestFieldShape<*> -> withParentFrame(ParentFrame(field.type.toString())) {
                field.generate(path)
            } as T
```

- [ ] **Step 4: Push the parent frame inside `generateSeededShape`**

In `generateSeededShape(...)`:

Replace the capture-pass block:
```kotlin
            val seed = withFrame(captures, capture) {
                withShapeDepth { field.generate(path) }
                capture.seed ?: error("Failed to capture @Seed value at $path for field $seedFieldName")
            }
            return field.generate(listOf(seed))
```
with:
```kotlin
            val seed = withFrame(captures, capture) {
                withParentFrame(ParentFrame(field.type.toString())) {
                    withShapeDepth { field.generate(path) }
                }
                capture.seed ?: error("Failed to capture @Seed value at $path for field $seedFieldName")
            }
            return withParentFrame(ParentFrame(field.type.toString())) {
                field.generate(listOf(seed))
            }
```

Replace the pending-seed branch (last lines of `generateSeededShape`):
```kotlin
        return withFrame(pendingSeeds, PendingSeed(candidate, seedFieldName, path)) {
            withShapeDepth { field.generate(path) }
        }
```
with:
```kotlin
        return withFrame(pendingSeeds, PendingSeed(candidate, seedFieldName, path)) {
            withParentFrame(ParentFrame(field.type.toString())) {
                withShapeDepth { field.generate(path) }
            }
        }
```

`shapeDepth` and the `shapeDepth == 0` checks are **not** changed. `parentStack` is additive; it tracks the enclosing shape independently of the @Seed depth counter.

- [ ] **Step 5: Run the full test suite to confirm no regressions**

```bash
./gradlew :src:integration:kotest:allTests
```

Expected: green. `parentStack` is unused at this point — every existing branch still behaves exactly as before, with `shapeDepth` continuing to gate the `@Seed` precedence checks.

- [ ] **Step 6: Commit**

```bash
git add src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGenerator.kt
git commit -m "feat(integration/kotest): add parent-shape stack for upcoming field-override lookup

Tracks the enclosing KotestFieldShape's type name on every shape entry
(generateLeaf + generateSeededShape) without touching the existing
shapeDepth counter, which keeps gating @Seed precedence.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Thread `OverrideRegistry` + `RefinedWrapper` into the generator

Plumb the override registry and the wrapper into the `KotestWirespecGenerator` constructor and the `kotestGenerator(...)` factory, plus the builder. No lookup hooks yet — they slot in next two tasks.

**Files:**
- Modify: `src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGenerator.kt`

- [ ] **Step 1: Update `kotestGenerator` and `KotestWirespecGenerator` constructor**

Replace:
```kotlin
fun kotestGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): KotestGenerator {
    val builder = KotestWirespecGeneratorBuilder().apply(block)
    return KotestWirespecGenerator(seed)
}

class KotestWirespecGeneratorBuilder internal constructor()

internal class KotestWirespecGenerator(
    private val baseSeed: Long,
) : KotestGenerator {
```

with:

```kotlin
fun kotestGenerator(
    seed: Long = 0L,
    refinedWrapper: RefinedWrapper = IdentityRefinedWrapper,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): KotestGenerator {
    val builder = KotestWirespecGeneratorBuilder().apply(block)
    return KotestWirespecGenerator(seed, builder.overrides, refinedWrapper)
}

class KotestWirespecGeneratorBuilder internal constructor() {
    internal val overrides: OverrideRegistry = OverrideRegistry()
}

internal class KotestWirespecGenerator(
    private val baseSeed: Long,
    private val overrides: OverrideRegistry,
    private val refinedWrapper: RefinedWrapper,
) : KotestGenerator {
```

- [ ] **Step 2: Run the full test suite**

```bash
./gradlew :src:integration:kotest:allTests
```

Expected: green. We added two unused private fields and a no-op default wrapper.

- [ ] **Step 3: Commit**

```bash
git add src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGenerator.kt
git commit -m "feat(integration/kotest): thread OverrideRegistry and RefinedWrapper into generator

Plumbing only — no lookup hooks yet. Defaults keep current behavior:
empty registry and identity wrapper.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Add path override lookup + builder method

Add `registerPath(...)` to the builder and the path-lookup hook between `@Seed` and the default-leaf path.

**Files:**
- Modify: `src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGenerator.kt`
- Create: `src/integration/kotest/src/commonTest/kotlin/community/flock/wirespec/integration/kotest/KotestOverrideTest.kt`

- [ ] **Step 1: Write failing tests for path override behavior**

Create `src/integration/kotest/src/commonTest/kotlin/community/flock/wirespec/integration/kotest/KotestOverrideTest.kt`:

```kotlin
package community.flock.wirespec.integration.kotest

import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class KotestOverrideTest {

    // ---------- registerPath ----------

    @Test
    fun `registerPath fires at the exact path`() {
        val gen = kotestGenerator(seed = 0L) {
            registerPath("users", "0", "id") { Arb.constant("FIXED-ID") }
        }
        val v = gen.generate(
            path = listOf("users", "0", "id"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        assertEquals("FIXED-ID", v)
    }

    @Test
    fun `registerPath with wildcard matches every array index`() {
        val gen = kotestGenerator(seed = 0L) {
            registerPath("users", "*", "id") { Arb.constant("WILD") }
        }
        repeat(5) { i ->
            val v = gen.generate(
                path = listOf("users", "$i", "id"),
                field = KotestFieldString(regex = null, annotations = emptyList()),
            )
            assertEquals("WILD", v, "expected wildcard match at index $i")
        }
    }

    @Test
    fun `registerPath does not fire on shorter or longer paths`() {
        val gen = kotestGenerator(seed = 0L) {
            registerPath("users", "0", "id") { Arb.constant("FIXED") }
        }
        val short = gen.generate(
            path = listOf("users", "0"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        val long = gen.generate(
            path = listOf("users", "0", "id", "x"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        assertNotEquals("FIXED", short)
        assertNotEquals("FIXED", long)
    }

    @Test
    fun `more specific path wins over wildcard`() {
        val gen = kotestGenerator(seed = 0L) {
            registerPath("users", "*", "id") { Arb.constant("WILD") }
            registerPath("users", "0", "id") { Arb.constant("EXACT") }
        }
        val v0 = gen.generate(
            path = listOf("users", "0", "id"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        val v1 = gen.generate(
            path = listOf("users", "1", "id"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        assertEquals("EXACT", v0)
        assertEquals("WILD", v1)
    }

    @Test
    fun `equally specific overlapping path patterns throw at lookup`() {
        val gen = kotestGenerator(seed = 0L) {
            registerPath("users", "*", "id") { Arb.constant("A") }
            registerPath("*", "0", "id") { Arb.constant("B") }
        }
        val ex = assertFailsWith<IllegalStateException> {
            gen.generate(
                path = listOf("users", "0", "id"),
                field = KotestFieldString(regex = null, annotations = emptyList()),
            )
        }
        assertNotNull(ex.message)
        assertEquals(true, ex.message!!.contains("Ambiguous"))
    }

    @Test
    fun `registerPath value shorthand wraps in Arb constant`() {
        val gen = kotestGenerator(seed = 0L) {
            registerPath("users", "0", "id", value = "VALUE-FORM")
        }
        val v = gen.generate(
            path = listOf("users", "0", "id"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        assertEquals("VALUE-FORM", v)
    }

    // ---------- precedence ----------

    @Test
    fun `path override beats default leaf`() {
        val gen = kotestGenerator(seed = 0L) {
            registerPath("a", "b") { Arb.constant("OV") }
        }
        val v = gen.generate(
            path = listOf("a", "b"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        assertEquals("OV", v)
    }

    @Test
    fun `Seed beats path override`() {
        // @Seed handling runs before path overrides; the captured seed value
        // takes precedence even when a path override is registered.
        val gen = kotestGenerator(seed = 0L) {
            registerPath("my-project-id", "id") { Arb.constant("FROM-PATH") }
        }
        val seedAnnotation = mapOf("name" to "Seed", "parameters" to emptyMap<String, Any>())
        val shape = KotestFieldShape<Map<String, String>>(
            annotations = mapOf("id" to listOf(seedAnnotation)),
            generate = { p ->
                val id = gen.generate(
                    p + "id",
                    KotestFieldString(regex = null, annotations = listOf(seedAnnotation)),
                )
                mapOf("id" to id)
            },
            type = typeOf<Map<String, String>>(),
        )
        val result = gen.generate(listOf("my-project-id"), shape)
        assertEquals("my-project-id", result["id"], "@Seed must win over path override")
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :src:integration:kotest:jvmTest --tests "community.flock.wirespec.integration.kotest.KotestOverrideTest"
```

Expected: compilation failure — `registerPath` doesn't exist on the builder.

- [ ] **Step 3: Add `registerPath` to the builder**

In `KotestWirespecGenerator.kt`, replace the `KotestWirespecGeneratorBuilder` body with:

```kotlin
class KotestWirespecGeneratorBuilder internal constructor() {
    internal val overrides: OverrideRegistry = OverrideRegistry()

    fun registerPath(vararg segments: String, factory: () -> Arb<*>) {
        overrides.addPath(segments, factory)
    }

    fun registerPath(vararg segments: String, value: Any?) {
        overrides.addPath(segments) { Arb.constant(value) }
    }
}
```

Add the `Arb.constant` import at the top of the file:

```kotlin
import io.kotest.property.arbitrary.constant
```

- [ ] **Step 4: Wire the path override lookup hook into the generator**

In `KotestWirespecGenerator.generate(...)`, insert the path lookup **after** the `@Seed-driven seeded shape` branch and **before** the `return generateLeaf(field, path)` line:

```kotlin
        overrides.findPath(path)?.let { factory ->
            val drawn = factory().next(rsFor(path))
            @Suppress("UNCHECKED_CAST")
            return refinedWrapper.wrap(drawn, field, path) as T
        }

        return generateLeaf(field, path)
```

- [ ] **Step 5: Run the new tests**

```bash
./gradlew :src:integration:kotest:jvmTest --tests "community.flock.wirespec.integration.kotest.KotestOverrideTest"
```

Expected: all 8 tests pass.

- [ ] **Step 6: Run the full kotest module test suite**

```bash
./gradlew :src:integration:kotest:allTests
```

Expected: green.

- [ ] **Step 7: Commit**

```bash
git add src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGenerator.kt \
        src/integration/kotest/src/commonTest/kotlin/community/flock/wirespec/integration/kotest/KotestOverrideTest.kt
git commit -m "feat(integration/kotest): add registerPath override

Adds path-based field overrides with \"*\" wildcard support, slotted into
precedence between @Seed and the default leaf. Ambiguous (equally specific)
patterns throw at lookup time.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Add field override lookup + builder method

Add `registerFieldByTypeName` to the builder (the commonMain entry point — the reified JVM extension follows in Task 7) and the field-lookup hook after the path-lookup hook.

**Files:**
- Modify: `src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGenerator.kt`
- Modify: `src/integration/kotest/src/commonTest/kotlin/community/flock/wirespec/integration/kotest/KotestOverrideTest.kt`

- [ ] **Step 1: Append failing tests for field override behavior**

Append to `KotestOverrideTest.kt`:

```kotlin

    // ---------- registerFieldByTypeName ----------

    @Test
    fun `registerFieldByTypeName fires when the leaf is a direct child of a matching shape`() {
        val gen = kotestGenerator(seed = 0L) {
            registerFieldByTypeName(typeOf<Map<String, String>>().toString(), "email") {
                Arb.constant("a@b.com")
            }
        }
        val shape = KotestFieldShape<Map<String, String>>(
            annotations = emptyMap(),
            generate = { p ->
                val email = gen.generate(
                    p + "email",
                    KotestFieldString(regex = null, annotations = emptyList()),
                )
                mapOf("email" to email)
            },
            type = typeOf<Map<String, String>>(),
        )
        val result = gen.generate(listOf("u"), shape)
        assertEquals("a@b.com", result["email"])
    }

    @Test
    fun `registerFieldByTypeName does not fire for a different parent type`() {
        val gen = kotestGenerator(seed = 0L) {
            registerFieldByTypeName(typeOf<Map<String, String>>().toString(), "email") {
                Arb.constant("a@b.com")
            }
        }
        // Parent is List<String>, not Map<String, String> — override must not fire.
        val shape = KotestFieldShape<List<String>>(
            annotations = emptyMap(),
            generate = { p ->
                val email = gen.generate(
                    p + "email",
                    KotestFieldString(regex = null, annotations = emptyList()),
                )
                listOf(email)
            },
            type = typeOf<List<String>>(),
        )
        val result = gen.generate(listOf("u"), shape)
        assertNotEquals("a@b.com", result.first(), "different parent type should not match")
    }

    @Test
    fun `registerFieldByTypeName does not fire at the top level (no enclosing shape)`() {
        val gen = kotestGenerator(seed = 0L) {
            registerFieldByTypeName(typeOf<Map<String, String>>().toString(), "email") {
                Arb.constant("a@b.com")
            }
        }
        val v = gen.generate(
            path = listOf("email"),
            field = KotestFieldString(regex = null, annotations = emptyList()),
        )
        assertNotEquals("a@b.com", v, "field override must require an enclosing shape")
    }

    @Test
    fun `path override beats field override`() {
        val gen = kotestGenerator(seed = 0L) {
            registerFieldByTypeName(typeOf<Map<String, String>>().toString(), "email") {
                Arb.constant("FIELD")
            }
            registerPath("u", "email") { Arb.constant("PATH") }
        }
        val shape = KotestFieldShape<Map<String, String>>(
            annotations = emptyMap(),
            generate = { p ->
                mapOf(
                    "email" to gen.generate(
                        p + "email",
                        KotestFieldString(regex = null, annotations = emptyList()),
                    ),
                )
            },
            type = typeOf<Map<String, String>>(),
        )
        val result = gen.generate(listOf("u"), shape)
        assertEquals("PATH", result["email"], "path override must win over field override")
    }

    @Test
    fun `field value shorthand wraps in Arb constant`() {
        val gen = kotestGenerator(seed = 0L) {
            registerFieldByTypeName(typeOf<Map<String, String>>().toString(), "email", value = "v@x")
        }
        val shape = KotestFieldShape<Map<String, String>>(
            annotations = emptyMap(),
            generate = { p ->
                mapOf(
                    "email" to gen.generate(
                        p + "email",
                        KotestFieldString(regex = null, annotations = emptyList()),
                    ),
                )
            },
            type = typeOf<Map<String, String>>(),
        )
        assertEquals("v@x", gen.generate(listOf("u"), shape)["email"])
    }
```

- [ ] **Step 2: Run the new tests to verify they fail**

```bash
./gradlew :src:integration:kotest:jvmTest --tests "community.flock.wirespec.integration.kotest.KotestOverrideTest"
```

Expected: compilation failure — `registerFieldByTypeName` doesn't exist on the builder.

- [ ] **Step 3: Add `registerFieldByTypeName` to the builder**

Inside `KotestWirespecGeneratorBuilder` in `KotestWirespecGenerator.kt`, add:

```kotlin
    fun registerFieldByTypeName(typeName: String, name: String, factory: () -> Arb<*>) {
        overrides.addField(FieldKey(typeName, name), factory)
    }

    fun registerFieldByTypeName(typeName: String, name: String, value: Any?) {
        overrides.addField(FieldKey(typeName, name)) { Arb.constant(value) }
    }
```

- [ ] **Step 4: Wire the field override lookup hook**

In `KotestWirespecGenerator.generate(...)`, insert the field lookup **after** the path-lookup hook (from Task 5) and **before** `return generateLeaf(field, path)`:

```kotlin
        val parent = parentStack.lastOrNull()
        val leafName = path.lastOrNull()
        if (parent != null && leafName != null) {
            overrides.findField(FieldKey(parent.typeName, leafName))?.let { factory ->
                val drawn = factory().next(rsFor(path))
                @Suppress("UNCHECKED_CAST")
                return refinedWrapper.wrap(drawn, field, path) as T
            }
        }

        return generateLeaf(field, path)
```

- [ ] **Step 5: Run the new tests**

```bash
./gradlew :src:integration:kotest:jvmTest --tests "community.flock.wirespec.integration.kotest.KotestOverrideTest"
```

Expected: all field-override tests pass (and the original 8 path-override tests stay green).

- [ ] **Step 6: Run the full kotest module test suite**

```bash
./gradlew :src:integration:kotest:allTests
```

Expected: green.

- [ ] **Step 7: Commit**

```bash
git add src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGenerator.kt \
        src/integration/kotest/src/commonTest/kotlin/community/flock/wirespec/integration/kotest/KotestOverrideTest.kt
git commit -m "feat(integration/kotest): add registerFieldByTypeName override

Adds (parent type, field name) overrides keyed by the parent shape's
type-name string. Fires only when the leaf is a direct child of a
matching KotestFieldShape; path overrides win when both match.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: Add JVM reified `registerField<Parent>` extensions + `JvmRefinedWrapper`

Provide the ergonomic JVM API: `registerField<Parent>(name) { Arb<...> }` (reified), plus the reflective Refined-ctor wrapper that auto-wraps drawn values for Refined-typed fields.

**Files:**
- Create: `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestBuilderJvm.kt`
- Create: `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecKotlinGeneratorOverrideJvmTest.kt`

- [ ] **Step 1: Write failing tests for the reified API and Refined auto-wrap**

Create `src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecKotlinGeneratorOverrideJvmTest.kt`:

```kotlin
package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.long
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class KotestWirespecKotlinGeneratorOverrideJvmTest {

    data class FakeUser(val email: String, val age: Long)
    data class FakeOrder(val email: String)

    // Mimics a Wirespec-emitted Refined wrapper: single-arg primary ctor.
    data class FakeEmailAddress(val value: String)

    @Test
    fun `registerField with reified Parent passes through to a String field`() {
        val gen = kotestWirespecKotlinGenerator(seed = 0L) {
            registerField<FakeUser>("email") { Arb.constant("a@b.com") }
        }
        val shape = Wirespec.GeneratorFieldShape<FakeUser>(
            annotations = emptyMap(),
            generate = { p ->
                val email = gen.generate(
                    p + "email",
                    Wirespec.GeneratorFieldString(regex = null, annotations = emptyList()),
                )
                FakeUser(email = email, age = 0L)
            },
            type = typeOf<FakeUser>(),
        )
        val v = gen.generate(listOf("u"), shape)
        assertEquals("a@b.com", v.email)
    }

    @Test
    fun `registerField with Long value works for primitive field`() {
        val gen = kotestWirespecKotlinGenerator(seed = 0L) {
            registerField<FakeUser>("age", value = 42L)
        }
        val shape = Wirespec.GeneratorFieldShape<FakeUser>(
            annotations = emptyMap(),
            generate = { p ->
                val age = gen.generate(
                    p + "age",
                    Wirespec.GeneratorFieldInteger64(min = null, max = null, annotations = emptyList()),
                )
                FakeUser(email = "x", age = age)
            },
            type = typeOf<FakeUser>(),
        )
        val v = gen.generate(listOf("u"), shape)
        assertEquals(42L, v.age)
    }

    @Test
    fun `registerField on Refined-typed field auto-wraps the drawn primitive`() {
        val gen = kotestWirespecKotlinGenerator(seed = 0L) {
            // Field's Kotlin type is FakeEmailAddress (a Refined wrapper of String).
            // User provides the inner primitive; the JVM RefinedWrapper wraps it.
            registerField<FakeUser>("email") { Arb.constant("auto@wrap.com") }
        }
        // The shape for FakeUser's email is a KotestFieldShape<FakeEmailAddress>
        // whose generate callback wraps a String into FakeEmailAddress. The
        // override fires before the callback runs, so the drawn String must
        // be passed through the RefinedWrapper.
        val emailFieldShape = Wirespec.GeneratorFieldShape<FakeEmailAddress>(
            annotations = emptyMap(),
            generate = { p ->
                val value = gen.generate(
                    p + "value",
                    Wirespec.GeneratorFieldString(regex = null, annotations = emptyList()),
                )
                FakeEmailAddress(value)
            },
            type = typeOf<FakeEmailAddress>(),
        )
        val userShape = Wirespec.GeneratorFieldShape<Pair<FakeEmailAddress, Long>>(
            annotations = emptyMap(),
            generate = { p ->
                val email = gen.generate(p + "email", emailFieldShape)
                email to 0L
            },
            type = typeOf<FakeUser>(),
        )
        val (email, _) = gen.generate(listOf("u"), userShape)
        assertEquals(FakeEmailAddress("auto@wrap.com"), email)
    }

    @Test
    fun `Refined auto-wrap type mismatch throws with documented message`() {
        val gen = kotestWirespecKotlinGenerator(seed = 0L) {
            registerField<FakeUser>("email") { Arb.long(0L..10L) }
        }
        val emailFieldShape = Wirespec.GeneratorFieldShape<FakeEmailAddress>(
            annotations = emptyMap(),
            generate = { p ->
                FakeEmailAddress(
                    gen.generate(
                        p + "value",
                        Wirespec.GeneratorFieldString(regex = null, annotations = emptyList()),
                    ),
                )
            },
            type = typeOf<FakeEmailAddress>(),
        )
        val userShape = Wirespec.GeneratorFieldShape<Pair<FakeEmailAddress, Long>>(
            annotations = emptyMap(),
            generate = { p ->
                gen.generate(p + "email", emailFieldShape) to 0L
            },
            type = typeOf<FakeUser>(),
        )
        val ex = assertFailsWith<IllegalStateException> {
            gen.generate(listOf("u"), userShape)
        }
        assertNotNull(ex.message)
        assertEquals(true, ex.message!!.contains("Override at"))
        assertEquals(true, ex.message!!.contains("FakeEmailAddress"))
    }

    @Test
    fun `registerField does not fire for the same field name on a different parent type`() {
        val gen = kotestWirespecKotlinGenerator(seed = 0L) {
            registerField<FakeUser>("email") { Arb.constant("user@x") }
        }
        // FakeOrder also has an `email` field; the override must not match.
        val shape = Wirespec.GeneratorFieldShape<FakeOrder>(
            annotations = emptyMap(),
            generate = { p ->
                FakeOrder(
                    gen.generate(
                        p + "email",
                        Wirespec.GeneratorFieldString(regex = null, annotations = emptyList()),
                    ),
                )
            },
            type = typeOf<FakeOrder>(),
        )
        val v = gen.generate(listOf("o"), shape)
        assertEquals(false, v.email == "user@x", "different parent must not match")
    }
}
```

- [ ] **Step 2: Run the new tests to verify they fail**

```bash
./gradlew :src:integration:kotest:jvmTest --tests "community.flock.wirespec.integration.kotest.KotestWirespecKotlinGeneratorOverrideJvmTest"
```

Expected: compilation failure — `registerField` extension and `JvmRefinedWrapper` don't exist.

- [ ] **Step 3: Create `KotestBuilderJvm.kt`**

Create `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestBuilderJvm.kt`:

```kotlin
package community.flock.wirespec.integration.kotest

import io.kotest.property.Arb
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Reified `(parent type, field name)` override registration. The parent's
 * Kotlin type is captured as `typeOf<Parent>().toString()` and matched
 * against the equivalent string the generator computes from
 * `KotestFieldShape.type` at lookup time. Both sides use the same Kotlin
 * stdlib `KType.toString()` representation, so the strings match.
 */
inline fun <reified Parent : Any> KotestWirespecGeneratorBuilder.registerField(
    name: String,
    noinline factory: () -> Arb<*>,
) {
    registerFieldByTypeName(typeOf<Parent>().toString(), name, factory)
}

inline fun <reified Parent : Any> KotestWirespecGeneratorBuilder.registerField(
    name: String,
    value: Any?,
) {
    registerFieldByTypeName(typeOf<Parent>().toString(), name, value)
}

/**
 * JVM-side `RefinedWrapper` that wraps a drawn primitive into the matching
 * single-arg Refined wrapper class. If the underlying [field] is not a
 * `KotestFieldShape<*>` with a single-ctor classifier, the drawn value is
 * passed through unchanged.
 */
internal object JvmRefinedWrapper : RefinedWrapper {

    private val cache = ConcurrentHashMap<KType, KFunction<Any>?>()

    @Suppress("UNCHECKED_CAST")
    private fun ctorFor(type: KType): KFunction<Any>? = cache.getOrPut(type) {
        val cls = (type.classifier as? KClass<*>) ?: return@getOrPut null
        cls.constructors.singleOrNull()
            ?.takeIf { it.parameters.size == 1 }
            ?.let { it as? KFunction<Any> }
    }

    override fun wrap(drawn: Any?, field: KotestField<*>, path: List<String>): Any? {
        val shape = field as? KotestFieldShape<*> ?: return drawn
        val ctor = ctorFor(shape.type) ?: return drawn
        return try {
            ctor.call(drawn)
        } catch (e: IllegalArgumentException) {
            error(
                "Override at ${path.joinToString("/")}: expected " +
                    "Arb<${ctor.parameters[0].type}> for refined " +
                    "${(shape.type.classifier as KClass<*>).qualifiedName}, " +
                    "got value of type ${drawn?.let { it::class.qualifiedName }}",
            )
        }
    }
}
```

- [ ] **Step 4: Wire `JvmRefinedWrapper` into `kotestWirespecKotlinGenerator`**

In `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecKotlinGenerator.kt`, change the factory body. Replace:

```kotlin
fun kotestWirespecKotlinGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Wirespec.Generator = WirespecKotlinGeneratorAdapter(kotestGenerator(seed, block))
```

with:

```kotlin
fun kotestWirespecKotlinGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Wirespec.Generator = WirespecKotlinGeneratorAdapter(
    kotestGenerator(seed, refinedWrapper = JvmRefinedWrapper, block = block),
)
```

- [ ] **Step 5: Run the new tests**

```bash
./gradlew :src:integration:kotest:jvmTest --tests "community.flock.wirespec.integration.kotest.KotestWirespecKotlinGeneratorOverrideJvmTest"
```

Expected: all 5 tests pass.

- [ ] **Step 6: Run the full kotest module test suite**

```bash
./gradlew :src:integration:kotest:allTests
```

Expected: green.

- [ ] **Step 7: Commit**

```bash
git add src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestBuilderJvm.kt \
        src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecKotlinGenerator.kt \
        src/integration/kotest/src/jvmTest/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecKotlinGeneratorOverrideJvmTest.kt
git commit -m "feat(integration/kotest): add reified registerField<Parent> + Refined auto-wrap

Reified JVM extensions capture the parent type via typeOf<Parent>().toString().
JvmRefinedWrapper inspects KotestFieldShape.type at draw time and invokes
the single-arg ctor for Refined wrappers; mismatches throw with a clear
error message.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: Wire `JvmRefinedWrapper` into Java and Scala JVM factories

The Java and Scala JVM factories also need the reflective wrapper so that downstream Java- or Scala-emitted Refined classes auto-wrap correctly.

**Files:**
- Modify: `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecJavaGenerator.kt`
- Modify: `src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecScalaGenerator.kt`

- [ ] **Step 1: Update `kotestWirespecJavaGenerator` to pass `JvmRefinedWrapper`**

Replace:

```kotlin
fun kotestWirespecJavaGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Wirespec.Generator = WirespecJavaGeneratorAdapter(kotestGenerator(seed, block))
```

with:

```kotlin
fun kotestWirespecJavaGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Wirespec.Generator = WirespecJavaGeneratorAdapter(
    kotestGenerator(seed, refinedWrapper = JvmRefinedWrapper, block = block),
)
```

- [ ] **Step 2: Update `kotestWirespecScalaGenerator` to pass `JvmRefinedWrapper`**

Replace:

```kotlin
fun kotestWirespecScalaGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Any = WirespecScalaGeneratorAdapter.create(kotestGenerator(seed, block))
```

with:

```kotlin
fun kotestWirespecScalaGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Any = WirespecScalaGeneratorAdapter.create(
    kotestGenerator(seed, refinedWrapper = JvmRefinedWrapper, block = block),
)
```

- [ ] **Step 3: Run the full kotest module test suite**

```bash
./gradlew :src:integration:kotest:allTests
```

Expected: green (no behavioral change against current tests — `JvmRefinedWrapper` only fires when an override hits and the field is a Refined-shaped `KotestFieldShape`, which neither the Java nor Scala adapter tests exercise yet).

- [ ] **Step 4: Commit**

```bash
git add src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecJavaGenerator.kt \
        src/integration/kotest/src/jvmMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecScalaGenerator.kt
git commit -m "feat(integration/kotest): install JvmRefinedWrapper in Java and Scala factories

Java and Scala IR-emitted callers benefit from the same auto-wrap when
users register an override for a Refined field. The wrapper inspects the
Kotlin-side KotestFieldShape.type, which is what both adapters already
carry across the boundary.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: Self-review and final verification

- [ ] **Step 1: Re-read the spec and verify every requirement has a corresponding task**

```bash
cat docs/superpowers/specs/2026-05-19-kotest-generator-overrides-design.md
```

Walk the spec section-by-section against the tasks:

| Spec requirement | Implementing task |
|---|---|
| Removal of `register(name)` / `DEFAULT_ARBS` / `@Generator` lookup | Task 1 |
| `registerFieldByTypeName` on builder (commonMain) | Task 6 |
| `registerPath(vararg)` on builder (commonMain) | Task 5 |
| Value-shorthand overloads | Tasks 5 & 6 |
| Reified `registerField<Parent>` on JVM | Task 7 |
| Parent-shape stack | Task 3 |
| Lookup precedence: `@Seed` → path → field → default | Tasks 5 & 6 (hooks slotted in order) |
| Path matching with `*` wildcard + specificity | Task 2 (data) + Task 5 (lookup) |
| Field matching keyed by `(parentTypeName, fieldName)` | Task 2 (data) + Task 6 (lookup) |
| Ambiguous-path error at lookup | Task 2 (logic) + Task 5 (tested) |
| Refined auto-wrap via SPI (`RefinedWrapper`) | Task 2 (interface) + Task 7 (JVM impl) |
| Identity wrapper for JS / default | Task 2 |
| Type-mismatch error message for Refined wrap | Task 7 |
| JS facade drops `registrations` parameter | Task 1 (Step 8) |

Any gaps go in as additional steps to the relevant task.

- [ ] **Step 2: Run the full multiplatform test suite**

```bash
./gradlew :src:integration:kotest:allTests
```

Expected: every test passes — JVM, JS, common.

- [ ] **Step 3: Confirm no leftover references to `@Generator` lookup**

```bash
grep -rn "Generator\"" src/integration/kotest/src --include="*.kt" | grep -v "/build/"
grep -rn "DEFAULT_ARBS\|namedArbs\|namedGeneratorOrNull" src/integration/kotest/src --include="*.kt" | grep -v "/build/"
grep -rn "register(\"" src/integration/kotest/src --include="*.kt" | grep -v "/build/"
```

Expected: no matches (the only `Generator` strings left should be in test fixtures explicitly verifying that `@Generator` is ignored, if any — there should be none after Task 1).

- [ ] **Step 4: Confirm KDoc in `KotestWirespecGenerator.kt` reflects the new API**

```bash
grep -A 30 "^fun kotestGenerator" src/integration/kotest/src/commonMain/kotlin/community/flock/wirespec/integration/kotest/KotestWirespecGenerator.kt
```

Expected: the KDoc above `kotestGenerator` mentions `registerField` and `registerPath` (it was updated in Task 1, Step 3). If it still mentions `@Generator` or `register(...)` as a real feature, fix it.

- [ ] **Step 5: Run spotless and any other static checks**

```bash
./gradlew :src:integration:kotest:spotlessCheck
```

If it fails, run:

```bash
./gradlew :src:integration:kotest:spotlessApply
```

Then re-run tests:

```bash
./gradlew :src:integration:kotest:allTests
```

- [ ] **Step 6: Final commit if any cleanup happened**

If Step 5 produced changes:

```bash
git add -A src/integration/kotest
git commit -m "style(integration/kotest): apply spotless formatting

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

Otherwise skip.

---

## Self-review notes

**Spec coverage:** All spec requirements map to a task in the table above. The "Removed tests" list in the spec is realized across Task 1 (steps 4–7, 9).

**Placeholder scan:** No "TBD" / "TODO" / "implement later" / "add appropriate error handling" remains; every code block is the actual content to write.

**Type consistency:** `OverrideRegistry`, `PathPattern`, `PathSegment`, `FieldKey`, `RefinedWrapper`, `IdentityRefinedWrapper`, `JvmRefinedWrapper`, `ParentFrame`, `registerPath`, `registerFieldByTypeName`, `registerField<Parent>` are introduced in Task 2 / 3 / 5 / 6 / 7 and used consistently with the same signatures in every subsequent task and test.
