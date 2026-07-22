package community.flock.wirespec.integration.kotest

import community.flock.kotlinx.rgxgen.RgxGen
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.next

/**
 * `KotestGenerator` backed by Kotest [Arb]s. `@Seed` annotations are honored for
 * deterministic array-element regeneration; `@Generator(...)` annotations are ignored —
 * use a scoped override ([KotestWirespecGeneratorBuilder.registerPath] /
 * [KotestWirespecGeneratorBuilder.registerFieldByTypeName]) instead.
 *
 * Not thread-safe: it keeps per-call traversal state, so create one per test rather than
 * sharing across concurrent tests (instances are cheap).
 */
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

    /**
     * Override the value generated at an exact path. `*` matches any single
     * segment (e.g. an array index): `registerPath("users", "*", "id") { ... }`.
     */
    fun registerPath(vararg segments: String, factory: () -> Gen<*>) {
        overrides.addPath(segments, factory)
    }

    /**
     * Constant-value form of [registerPath]. The `value` argument **must be
     * named** — `registerPath("users", "id", "FIXED")` would register the
     * three-segment path `users/id/FIXED` instead, because the vararg
     * swallows positional strings.
     */
    fun registerPath(vararg segments: String, value: Any?) {
        overrides.addPath(segments) { Arb.constant(value) }
    }

    fun registerFieldByTypeName(typeName: String, name: String, factory: () -> Gen<*>) {
        overrides.addField(FieldKey(typeName, name), factory)
    }

    fun registerFieldByTypeName(typeName: String, name: String, value: Any?) {
        overrides.addField(FieldKey(typeName, name)) { Arb.constant(value) }
    }
}

internal class KotestWirespecGenerator(
    private val baseSeed: Long,
    private val overrides: OverrideRegistry,
    private val refinedWrapper: RefinedWrapper,
) : KotestGenerator {

    private companion object {
        // Nesting depth past which generated lists are emptied to terminate self-recursive types.
        // Generous enough for realistic contract nesting; only deep runaway recursion is bounded.
        const val MAX_GEN_DEPTH = 12
    }

    private val pendingSeeds = ArrayDeque<PendingSeed>()

    private val captures = ArrayDeque<Capture>()

    private var shapeDepth = 0

    // Actual value-generation nesting depth (per shape/array descent), used to bound self-recursive
    // types beyond [MAX_GEN_DEPTH]. Distinct from [shapeDepth], which only gates @Seed handling.
    private var genDepth = 0

    private inline fun <R> withGenDepth(block: () -> R): R {
        genDepth++
        return try {
            block()
        } finally {
            genDepth--
        }
    }

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

    private data class PendingSeed(val value: String, val target: String, val pathPrefix: List<String>)

    private class Capture(val shapePath: List<String>, val fieldName: String) {
        var seed: String? = null
    }

    // FNV-1a (64-bit) over the joined path: String.hashCode is only 32 bits
    // and collides easily ("Aa"/"BB"), which would give unrelated fields
    // identical values.
    private fun pathHash(path: List<String>): Long {
        var hash = -3750763034362895579L // FNV-1a 64-bit offset basis
        for (segment in path) {
            for (ch in segment) {
                hash = hash xor ch.code.toLong()
                hash *= 1099511628211L // FNV-1a 64-bit prime
            }
            hash = hash xor '/'.code.toLong()
            hash *= 1099511628211L
        }
        return hash
    }

    private fun rsFor(path: List<String>): RandomSource = RandomSource.seeded(baseSeed xor pathHash(path))

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

        overrides.findPath(path)?.let { factory -> return applyOverride(factory, field, path) }

        val parent = parentStack.lastOrNull()
        val leafName = path.lastOrNull()
        if (parent != null && leafName != null) {
            overrides.findField(FieldKey(parent.typeName, leafName))?.let { factory -> return applyOverride(factory, field, path) }
        }

        return generateLeaf(field, path)
    }

    /** Draw the override [factory]'s value for [path], re-wrapping it for a refined [field]. */
    @Suppress("UNCHECKED_CAST")
    private fun <T> applyOverride(factory: () -> Gen<*>, field: KotestField<T>, path: List<String>): T = refinedWrapper.wrap(factory().drawOne(rsFor(path)), field, path) as T

    private fun captureSeedIfMatches(path: List<String>, field: KotestField<*>): Any? {
        val capture = captures.lastOrNull() ?: return null
        if (capture.seed != null) return null
        val expectedPrefix = capture.shapePath + capture.fieldName
        if (path.size < expectedPrefix.size || path.subList(0, expectedPrefix.size) != expectedPrefix) return null
        val rs = rsFor(path)
        return when (field) {
            is KotestFieldString -> generateString(field, path, rs).also { capture.seed = it }
            is KotestFieldInteger64 -> Arb.long((field.min ?: 0)..(field.max ?: Long.MAX_VALUE)).next(rs).also { capture.seed = it.toString() }
            is KotestFieldInteger32 -> Arb.int((field.min ?: 0)..(field.max ?: Int.MAX_VALUE)).next(rs).also { capture.seed = it.toString() }
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
                withParentFrame(ParentFrame(field.type.toString())) {
                    withShapeDepth { field.generate(path) }
                }
                capture.seed ?: error("Failed to capture @Seed value at $path for field $seedFieldName")
            }
            return withParentFrame(ParentFrame(field.type.toString())) {
                field.generate(listOf(seed))
            }
        }

        val candidate = path.dropLast(1).lastOrNull() ?: return null
        return withFrame(pendingSeeds, PendingSeed(candidate, seedFieldName, path)) {
            withParentFrame(ParentFrame(field.type.toString())) {
                withShapeDepth { field.generate(path) }
            }
        }
    }

    private fun seedFieldNameOf(field: KotestFieldShape<*>): String? = field.annotations.entries
        .firstOrNull { (_, anns) -> anns.any { it["name"] == "Seed" } }
        ?.key

    /** With a regex constraint, matches it verbatim; otherwise a readable `<fieldName>-XXXXXXXX` token. */
    private fun generateString(field: KotestFieldString, path: List<String>, rs: RandomSource): String {
        val prefix = if (field.regex != null) "" else path.lastOrNull().orEmpty() + "-"
        val regex = field.regex ?: "\\w{8}"
        return prefix + RgxGen.parse(regex).generate(rs.random)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> generateLeaf(field: KotestField<T>, path: List<String>): T {
        val rs = rsFor(path)
        return when (field) {
            is KotestFieldString -> generateString(field, path, rs) as T
            is KotestFieldInteger64 -> Arb.long((field.min ?: Long.MIN_VALUE)..(field.max ?: Long.MAX_VALUE)).next(rs) as T
            is KotestFieldInteger32 -> Arb.int((field.min ?: Int.MIN_VALUE)..(field.max ?: Int.MAX_VALUE)).next(rs) as T
            is KotestFieldNumber64 -> Arb.double(field.min ?: -1e6, field.max ?: 1e6).next(rs) as T
            is KotestFieldNumber32 -> Arb.float(field.min ?: -1e6f, field.max ?: 1e6f).next(rs) as T
            is KotestFieldBoolean -> Arb.boolean().next(rs) as T
            is KotestFieldBytes -> Arb.byteArray(Arb.int(0..16), Arb.byte()).next(rs) as T
            is KotestFieldEnum -> Arb.element(field.values).next(rs) as T
            is KotestFieldUnion -> Arb.element(field.variants).next(rs) as T
            is KotestFieldArray<*> -> {
                // Empty deep lists so self-recursive element types (a parameter holding a list of
                // parameters) terminate instead of overflowing the stack.
                val size = if (genDepth >= MAX_GEN_DEPTH) 0 else Arb.int(1..10).next(rs)
                (0 until size).map { i -> withGenDepth { field.generate(path + "$i") } } as T
            }
            // Deterministic ~20% null chance so consumers' null branches are
            // exercised; same seed + path always reproduces the same choice.
            is KotestFieldNullable<*> -> if (Arb.int(0..4).next(rs) == 0) null as T else field.generate(path) as T
            is KotestFieldShape<*> -> withParentFrame(ParentFrame(field.type.toString())) {
                withGenDepth { field.generate(path) }
            } as T
            is KotestFieldDict<*> -> mapOf("a" to field.generate(path + "a")) as T
        }
    }

    private fun KotestField<*>.fieldAnnotations(): List<Map<String, Any>> = (this as? KotestLeafField<*>)?.annotations ?: emptyList()
}

/** Draws a single value from any [Gen] (both [Arb] and `Exhaustive`), seeded by [rs]. */
private fun <A> Gen<A>.drawOne(rs: RandomSource): A = generate(rs).first().value

private inline fun <F, R> withFrame(stack: ArrayDeque<F>, frame: F, block: () -> R): R {
    val mark = stack.size
    stack.addLast(frame)
    return try {
        block()
    } finally {
        while (stack.size > mark) stack.removeLast()
    }
}
