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

fun interface RefinedWrapper {
    fun wrap(drawn: Any?, field: KotestField<*>, path: List<String>): Any?
}

object IdentityRefinedWrapper : RefinedWrapper {
    override fun wrap(drawn: Any?, field: KotestField<*>, path: List<String>): Any? = drawn
}
