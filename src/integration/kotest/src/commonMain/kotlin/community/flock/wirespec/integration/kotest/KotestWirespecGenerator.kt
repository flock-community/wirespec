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
            is KotestFieldShape<*> -> withParentFrame(ParentFrame(field.type.toString())) {
                field.generate(path)
            } as T
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
