package community.flock.wirespec.integration.kotest

import community.flock.kotlinx.rgxgen.RgxGen
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.next

/**
 * `KotestGenerator` implementation backed by Kotest [Arb]s. Drive the
 * IR-emitted `*Generator.generate(...)` factories with this in your tests:
 *
 * ```
 * val gen = kotestGenerator(seed = 1L) {
 *     register("orderId") { Arb.uuid().map(java.util.UUID::toString) }
 * }
 * val member = MemberGenerator.generate(gen, emptyList())
 * ```
 *
 * On the JVM you typically get a `Wirespec.Generator` directly via
 * `kotestWirespecGenerator(...)` (jvmMain), which wraps this commonMain
 * factory. On Kotlin/JS, [kotestWirespecGeneratorJs] does the same wrapping
 * with a dynamic boundary.
 *
 * Field-level dispatch:
 * - `@Generator("name")` looks `name` up case-insensitively in the registry.
 * - `@Seed` on a child field of a `Shape` is honored: the seed value is taken
 *   from the parent path (or, inside an array context, captured on a first
 *   pass and replayed on a second pass) so test data can be regenerated
 *   deterministically given just an ID.
 * - Otherwise, each `KotestField<T>` variant maps to a sensible default
 *   `Arb<T>` (`Arb.stringPattern(regex)`, `Arb.long(min..max)`, …).
 *
 * Determinism: each leaf draws from a [RandomSource] keyed on
 * `seed XOR hashCode(path)`. Changing the path (e.g. via a different `@Seed`
 * id) reshuffles every sibling field, so two records with different ids are
 * guaranteed to differ in their content.
 */
fun kotestGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): KotestGenerator {
    val builder = KotestWirespecGeneratorBuilder().apply {
        // Preinstall the curated default catalog. User registrations in
        // `block` override these by lowercase-name match.
        DEFAULT_ARBS.forEach { (name, factory) -> register(name, factory) }
        block()
    }
    return KotestWirespecGenerator(seed, builder.build())
}

class KotestWirespecGeneratorBuilder internal constructor() {
    private val registrations = mutableMapOf<String, () -> Arb<String>>()

    /**
     * Register an [Arb] under [name] (matched case-insensitively against the
     * `default` parameter of `@Generator(...)`). The factory is invoked
     * lazily per generation to keep `Arb` building free of side effects.
     */
    fun register(name: String, factory: () -> Arb<String>) {
        registrations[name.lowercase()] = factory
    }

    internal fun build(): Map<String, () -> Arb<String>> = registrations.toMap()
}

internal class KotestWirespecGenerator(
    private val baseSeed: Long,
    private val namedArbs: Map<String, () -> Arb<String>>,
) : KotestGenerator {

    private val pendingSeeds = ArrayDeque<PendingSeed>()

    private val captures = ArrayDeque<Capture>()

    // Tracks descent into nested `KotestFieldShape.generate(...)` callbacks
    // that cross a generator boundary. `@Seed` is only honored at the
    // top-most level — i.e. the shape the user invoked directly via
    // `XGenerator.generate(gen, [seed])`. When a parent generator forwards
    // its path into a nested generator (e.g. `Project.owner: Member`), the
    // path's leading slot is the *parent's* seed (or its field name), not a
    // user-supplied seed for the nested type, so the inner generator's
    // `@Seed` field falls through to a deterministic random value keyed on
    // the path. See KotestWirespecGeneratorTest's `nested Shape with @Seed …`
    // cases.
    private var shapeDepth = 0

    // `pathPrefix` is the path at which the seed was pushed. Consumption
    // requires the consumer's `path` to equal `pathPrefix + [target]`
    // exactly, which prevents a parent-scoped `PendingSeed` from being
    // drained by an unrelated descendant of a nested shape.
    private data class PendingSeed(val value: String, val target: String, val pathPrefix: List<String>)

    private class Capture(val shapePath: List<String>, val fieldName: String) {
        var seed: String? = null
    }

    private fun rsFor(path: List<String>): RandomSource = RandomSource.seeded(baseSeed xor path.joinToString("/").hashCode().toLong())

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

        // Direct-`@Seed`-on-primitive: only honored at the top level. Inside
        // a nested generator, the path's parent slot is a field name, not a
        // user-supplied seed.
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

        annotations.namedGeneratorOrNull()?.let { name ->
            val factory = namedArbs[name.lowercase()]
                ?: error("Unknown @Generator name: '$name' — register it via kotestGenerator { register(\"$name\") { … } }")
            return factory().next(rsFor(path)) as T
        }

        return generateLeaf(field, path)
    }

    /**
     * Generate (and capture) the @Seed value at its natural path during the
     * first pass of an array-element two-pass. The captured value is always
     * stored as a String (paths are `List<String>`); for an Integer @Seed we
     * also return the parsed Long so the field receives its native type.
     */
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
            is KotestFieldInteger -> {
                val lo = field.min ?: 0
                val hi = field.max ?: Long.MAX_VALUE
                val value = Arb.long(lo..hi).next(rs)
                capture.seed = value.toString()
                value
            }
            else -> null
        }
    }

    private fun consumePendingSeedIfMatches(path: List<String>, field: KotestField<*>): Any? {
        val pending = pendingSeeds.lastOrNull() ?: return null
        // Strict scope check: only fire when this leaf is the immediate
        // child of the seed's pathPrefix, named exactly `target`. This stops
        // an outer pending from being claimed by a deeper descendant of a
        // sibling/nested shape (e.g. project's seed being slurped by
        // `owner.id.value` inside a nested `Member`).
        if (path.size != pending.pathPrefix.size + 1) return null
        if (path.last() != pending.target) return null
        if (path.subList(0, pending.pathPrefix.size) != pending.pathPrefix) return null
        return when (field) {
            is KotestFieldString -> {
                pendingSeeds.removeLast()
                pending.value
            }
            is KotestFieldInteger -> {
                pendingSeeds.removeLast()
                pending.value.toLong()
            }
            else -> null
        }
    }

    /**
     * Direct-`@Seed`-on-primitive case (no Refined wrapper): pull the seed
     * from the parent path segment and coerce to the field's native type.
     */
    private fun seedAnnotationValueFor(field: KotestField<*>, candidate: String): Any? = when (field) {
        is KotestFieldString -> candidate
        is KotestFieldInteger -> candidate.toLongOrNull()
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
                // Capture pass crosses the generator boundary, so increment
                // depth to suppress nested @Seed re-extraction.
                withShapeDepth { field.generate(path) }
                capture.seed ?: error("Failed to capture @Seed value at $path for field $seedFieldName")
            }
            // Replay pass is conceptually a fresh top-level call with
            // path=[seed], so it stays at depth 0 — that lets the inner
            // @Seed extract the captured value the normal way.
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

    private fun List<Map<String, Any>>.namedGeneratorOrNull(): String? = firstOrNull { it["name"] == "Generator" }
        ?.let { (it["parameters"] as? Map<*, *>)?.get("default") as? String }

    @Suppress("UNCHECKED_CAST")
    private fun <T> generateLeaf(field: KotestField<T>, path: List<String>): T {
        val rs = rsFor(path)
        return when (field) {
            is KotestFieldString -> {
                val prefix = field.regex?.let { "" } ?: (path.lastOrNull().orEmpty() + "-")
                val regex = field.regex ?: "\\w{8}"
                (prefix + RgxGen.parse(regex).generate(rs.random)) as T
            }
            is KotestFieldInteger -> {
                val lo = field.min ?: Long.MIN_VALUE
                val hi = field.max ?: Long.MAX_VALUE
                Arb.long(lo..hi).next(rs) as T
            }
            is KotestFieldNumber -> {
                val lo = field.min ?: -1e6
                val hi = field.max ?: 1e6
                Arb.double(lo, hi).next(rs) as T
            }
            is KotestFieldBoolean -> Arb.boolean().next(rs) as T
            is KotestFieldBytes -> Arb.byteArray(Arb.int(0..16), Arb.byte()).next(rs) as T
            is KotestFieldEnum -> Arb.element(field.values).next(rs) as T
            is KotestFieldUnion -> Arb.element(field.variants).next(rs) as T
            is KotestFieldArray<*> -> {
                val size = Arb.int(1..10).next(rs)
                (0 until size).map { i -> field.generate(path + "$i") } as T
            }
            // Match the existing SeededGenerator: always materialize the value rather
            // than randomly returning null. Tests that need null can do `.copy(field = null)`.
            is KotestFieldNullable<*> -> field.generate(path) as T
            is KotestFieldShape<*> -> field.generate(path) as T
            is KotestFieldDict<*> -> mapOf("a" to field.generate(path + "a")) as T
        }
    }

    private fun KotestField<*>.fieldAnnotations(): List<Map<String, Any>> = when (this) {
        is KotestFieldString -> annotations
        is KotestFieldInteger -> annotations
        is KotestFieldNumber -> annotations
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
