package community.flock.wirespec.integration.kotest

import community.flock.wirespec.kotlin.Wirespec
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
import io.kotest.property.arbitrary.stringPattern
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KType

/**
 * `Wirespec.Generator` implementation backed by Kotest [Arb]s. Drive the
 * IR-emitted `*Generator.generate(...)` factories with this in your tests:
 *
 * ```
 * val gen = kotestWirespecGenerator(seed = 1L) {
 *     register("orderId") { Arb.uuid().map(java.util.UUID::toString) }
 * }
 * val member = MemberGenerator.generate(gen, emptyList())
 * ```
 *
 * Field-level dispatch:
 * - `@Generator("name")` looks `name` up case-insensitively in the registry.
 * - `@Seed` on a child field of a `Shape` is honored: the seed value is taken
 *   from the parent path (or, inside an array context, captured on a first
 *   pass and replayed on a second pass) so test data can be regenerated
 *   deterministically given just an ID.
 * - Otherwise, each `Wirespec.GeneratorField<T>` variant maps to a sensible
 *   default `Arb<T>` (`Arb.stringPattern(regex)`, `Arb.long(min..max)`, …).
 *
 * Determinism: each leaf draws from a [RandomSource] keyed on
 * `seed XOR hashCode(path)`. Changing the path (e.g. via a different `@Seed`
 * id) reshuffles every sibling field, so two records with different ids are
 * guaranteed to differ in their content.
 */
fun kotestWirespecGenerator(
    seed: Long = 0L,
    block: KotestWirespecGeneratorBuilder.() -> Unit = {},
): Wirespec.Generator {
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
) : Wirespec.Generator {

    // Stack of seeds to inject into a deeper @Seed string. Each frame is pushed
    // by an enclosing Shape and consumed by the first matching string field.
    private val pendingSeeds = ArrayDeque<PendingSeed>()

    // Stack of active "capture an array-element's @Seed value" frames. Only
    // the top frame's @Seed string is captured; outer frames wait their turn.
    private val captures = ArrayDeque<Capture>()

    private data class PendingSeed(val value: String, val target: String)

    private class Capture(val shapePath: List<String>, val fieldName: String) {
        val seed: AtomicReference<String?> = AtomicReference(null)
    }

    private fun rsFor(path: List<String>): RandomSource = RandomSource.seeded(baseSeed xor path.joinToString("/").hashCode().toLong())

    @Suppress("UNCHECKED_CAST")
    override fun <T> generate(
        path: List<String>,
        type: KType,
        field: Wirespec.GeneratorField<T>,
    ): T {
        captureSeedIfMatches(path, field)?.let { return it as T }

        consumePendingSeedIfMatches(path, field)?.let { return it as T }

        val annotations = field.fieldAnnotations()

        if (annotations.any { it["name"] == "Seed" }) {
            path.dropLast(1).lastOrNull()?.let { candidate ->
                seedAnnotationValueFor(field, candidate)?.let { return it as T }
            }
        }

        if (field is Wirespec.GeneratorFieldShape<*>) {
            seedFieldNameOf(field)?.let { seedFieldName ->
                generateSeededShape(path, field, seedFieldName)?.let { return it as T }
            }
        }

        annotations.namedGeneratorOrNull()?.let { name ->
            val factory = namedArbs[name.lowercase()]
                ?: error("Unknown @Generator name: '$name' — register it via kotestWirespecGenerator { register(\"$name\") { … } }")
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
    private fun captureSeedIfMatches(path: List<String>, field: Wirespec.GeneratorField<*>): Any? {
        val capture = captures.lastOrNull() ?: return null
        if (capture.seed.get() != null) return null
        val expectedPrefix = capture.shapePath + capture.fieldName
        if (path.size < expectedPrefix.size || path.subList(0, expectedPrefix.size) != expectedPrefix) return null
        val rs = rsFor(path)
        return when (field) {
            is Wirespec.GeneratorFieldString -> {
                val regex = field.regex ?: "\\w{1,50}"
                val value = Arb.stringPattern(regex).next(rs)
                capture.seed.set(value)
                value
            }
            is Wirespec.GeneratorFieldInteger -> {
                val lo = field.min ?: 0
                val hi = field.max ?: Long.MAX_VALUE
                val value = Arb.long(lo..hi).next(rs)
                capture.seed.set(value.toString())
                value
            }
            else -> null
        }
    }

    private fun consumePendingSeedIfMatches(path: List<String>, field: Wirespec.GeneratorField<*>): Any? {
        val pending = pendingSeeds.lastOrNull() ?: return null
        if (pending.target !in path) return null
        return when (field) {
            is Wirespec.GeneratorFieldString -> {
                pendingSeeds.removeLast()
                pending.value
            }
            is Wirespec.GeneratorFieldInteger -> {
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
    private fun seedAnnotationValueFor(field: Wirespec.GeneratorField<*>, candidate: String): Any? = when (field) {
        is Wirespec.GeneratorFieldString -> candidate
        is Wirespec.GeneratorFieldInteger -> candidate.toLongOrNull()
        else -> null
    }

    private fun generateSeededShape(
        path: List<String>,
        field: Wirespec.GeneratorFieldShape<*>,
        seedFieldName: String,
    ): Any? {
        val isArrayContext = path.lastOrNull()?.toIntOrNull() != null
        if (isArrayContext && captures.isEmpty()) {
            val capture = Capture(path, seedFieldName)
            val seed = withFrame(captures, capture) {
                field.generate(path)
                capture.seed.get() ?: error("Failed to capture @Seed value at $path for field $seedFieldName")
            }
            return field.generate(listOf(seed))
        }

        val candidate = path.dropLast(1).lastOrNull() ?: return null
        return withFrame(pendingSeeds, PendingSeed(candidate, seedFieldName)) {
            field.generate(path)
        }
    }

    private fun seedFieldNameOf(field: Wirespec.GeneratorFieldShape<*>): String? = field.annotations.entries
        .firstOrNull { (_, anns) -> anns.any { it["name"] == "Seed" } }
        ?.key

    private fun List<Map<String, Any>>.namedGeneratorOrNull(): String? = firstOrNull { it["name"] == "Generator" }
        ?.let { (it["parameters"] as? Map<*, *>)?.get("default") as? String }

    @Suppress("UNCHECKED_CAST")
    private fun <T> generateLeaf(field: Wirespec.GeneratorField<T>, path: List<String>): T {
        val rs = rsFor(path)
        return when (field) {
            is Wirespec.GeneratorFieldString -> {
                val regex = field.regex ?: "\\w{1,50}"
                Arb.stringPattern(regex).next(rs) as T
            }
            is Wirespec.GeneratorFieldInteger -> {
                val lo = field.min ?: Long.MIN_VALUE
                val hi = field.max ?: Long.MAX_VALUE
                Arb.long(lo..hi).next(rs) as T
            }
            is Wirespec.GeneratorFieldNumber -> {
                val lo = field.min ?: -1e6
                val hi = field.max ?: 1e6
                Arb.double(lo, hi).next(rs) as T
            }
            is Wirespec.GeneratorFieldBoolean -> Arb.boolean().next(rs) as T
            is Wirespec.GeneratorFieldBytes -> Arb.byteArray(Arb.int(0..16), Arb.byte()).next(rs) as T
            is Wirespec.GeneratorFieldEnum -> Arb.element(field.values).next(rs) as T
            is Wirespec.GeneratorFieldUnion -> Arb.element(field.variants).next(rs) as T
            is Wirespec.GeneratorFieldArray<*> -> {
                val size = Arb.int(1..10).next(rs)
                (0 until size).map { i -> field.generate(path + "$i") } as T
            }
            // Match the existing SeededGenerator: always materialize the value rather
            // than randomly returning null. Tests that need null can do `.copy(field = null)`.
            is Wirespec.GeneratorFieldNullable<*> -> field.generate(path) as T
            is Wirespec.GeneratorFieldShape<*> -> field.generate(path) as T
            is Wirespec.GeneratorFieldDict<*> -> mapOf("a" to field.generate(path + "a")) as T
        }
    }

    private fun Wirespec.GeneratorField<*>.fieldAnnotations(): List<Map<String, Any>> = when (this) {
        is Wirespec.GeneratorFieldString -> annotations
        is Wirespec.GeneratorFieldInteger -> annotations
        is Wirespec.GeneratorFieldNumber -> annotations
        is Wirespec.GeneratorFieldBoolean -> annotations
        is Wirespec.GeneratorFieldBytes -> annotations
        is Wirespec.GeneratorFieldEnum -> annotations
        is Wirespec.GeneratorFieldUnion -> annotations
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
