package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type

internal data class EndpointShape(
    val name: String,
    val pathFields: List<NamedTypedField>,
    val queryFields: List<NamedTypedField>,
    val headerFields: List<NamedTypedField>,
    /** Full Kotlin type of the body, e.g. `"Pet"` (Object) or `"List<Pet>"` (List). `null` when there is no body. */
    val bodyType: String?,
    val bodyKind: BodyKind,
    /**
     * Name of the body's element Type — `"Pet"` for both `Object` (body is `Pet`) and `List` (body is `List<Pet>`).
     * `null` for `BodyKind.None`. **Prefer this over `bodyType` when constructing builder class names**, because
     * `bodyType` is `"List<T>"` for list bodies and not a valid Kotlin identifier segment.
     */
    val bodyElementType: String?,
    /**
     * Recursive classification of body fields. Each field is one of:
     *  - [BodyFieldShape.Primitive] — leaf, declared as `Arb<KotlinType>?`
     *  - [BodyFieldShape.NestedObject] — drills into a known custom [Type], emits `<field> { … }` overload
     *  - [BodyFieldShape.NestedList] — `Iterable<Custom>` of a known [Type], registers paths with `"*"` segment
     */
    val bodyFieldShapes: List<BodyFieldShape>,
    val modelImports: List<String>,
) {
    /** Flat top-level view of [bodyFieldShapes], kept for backwards-compat with callers that expect a simple list. */
    val bodyFields: List<NamedTypedField> = bodyFieldShapes.map { f ->
        NamedTypedField(
            f.name,
            when (f) {
                is BodyFieldShape.Primitive -> f.kotlinType
                is BodyFieldShape.NestedObject -> f.typeName
                is BodyFieldShape.NestedList -> "List<${f.elementTypeName}>"
            },
        )
    }

    data class NamedTypedField(val name: String, val kotlinType: String, val isNullable: Boolean = false)

    enum class BodyKind { None, Object, List }

    sealed interface BodyFieldShape {
        val name: String

        data class Primitive(override val name: String, val kotlinType: String) : BodyFieldShape
        data class NestedObject(
            override val name: String,
            val typeName: String,
            val fields: List<BodyFieldShape>,
        ) : BodyFieldShape
        data class NestedList(
            override val name: String,
            val elementTypeName: String,
            val fields: List<BodyFieldShape>,
        ) : BodyFieldShape
    }

    companion object {
        fun from(
            endpoint: Endpoint,
            types: Map<String, Type> = emptyMap(),
            refined: Map<String, Refined> = emptyMap(),
        ): EndpointShape {
            val pathFields = endpoint.path
                .filterIsInstance<Endpoint.Segment.Param>()
                .map { NamedTypedField(it.identifier.value, KotlinTypeMapper.map(it.reference), it.reference.isNullable) }
            val queryFields = endpoint.queries
                .map { NamedTypedField(it.identifier.value, KotlinTypeMapper.map(it.reference), it.reference.isNullable) }
            val headerFields = endpoint.headers
                .map { NamedTypedField(it.identifier.value, KotlinTypeMapper.map(it.reference), it.reference.isNullable) }
            val bodyRef = endpoint.requests.firstOrNull()?.content?.reference
            val bodyType = bodyRef?.let { if (it is Reference.Unit) null else KotlinTypeMapper.map(it) }

            val (bodyKind, elementCustomName) = when (bodyRef) {
                null, is Reference.Unit -> BodyKind.None to null
                is Reference.Custom -> BodyKind.Object to bodyRef.value
                is Reference.Iterable -> {
                    val inner = bodyRef.reference
                    if (inner is Reference.Custom) BodyKind.List to inner.value else BodyKind.None to null
                }
                else -> BodyKind.None to null
            }
            val bodyElementType = elementCustomName

            // Body-field kotlinType unwraps refined wrappers to their base primitive: the typed
            // body{} builder declares the field as Arb<BaseType>, and the runtime's RefinedWrapper
            // wraps each drawn primitive into the refined class via its single-arg ctor. Without
            // this unwrap, overriding a refined field at runtime throws "expected Arb<BaseType>
            // for refined …, got value of type …".
            val bodyFieldShapes: List<BodyFieldShape> = elementCustomName
                ?.let { extractBodyFields(it, types, refined, visited = emptySet()) }
                ?: emptyList()

            val refs = buildList {
                endpoint.path.filterIsInstance<Endpoint.Segment.Param>().forEach { add(it.reference) }
                endpoint.queries.forEach { add(it.reference) }
                endpoint.headers.forEach { add(it.reference) }
                if (bodyRef != null) add(bodyRef)
            }
            val bodyFieldRefs = elementCustomName
                ?.let { types[it] }
                ?.shape?.value
                ?.map { it.reference }
                ?: emptyList()
            val modelImports = (
                (refs + bodyFieldRefs).flatMap(::collectCustomNames) +
                    collectNestedTypeNames(bodyFieldShapes) +
                    collectFieldTypeNames(bodyFieldShapes, types)
                ).distinct()

            return EndpointShape(
                name = endpoint.identifier.value,
                pathFields = pathFields,
                queryFields = queryFields,
                headerFields = headerFields,
                bodyType = bodyType,
                bodyKind = bodyKind,
                bodyElementType = bodyElementType,
                bodyFieldShapes = bodyFieldShapes,
                modelImports = modelImports,
            )
        }

        private fun collectCustomNames(reference: Reference): List<String> = when (reference) {
            is Reference.Custom -> listOf(reference.value)
            is Reference.Iterable -> collectCustomNames(reference.reference)
            is Reference.Dict -> collectCustomNames(reference.reference)
            else -> emptyList()
        }

        private fun collectNestedTypeNames(fields: List<BodyFieldShape>): List<String> = fields.flatMap { f ->
            when (f) {
                is BodyFieldShape.Primitive -> emptyList()
                is BodyFieldShape.NestedObject -> listOf(f.typeName) + collectNestedTypeNames(f.fields)
                is BodyFieldShape.NestedList -> listOf(f.elementTypeName) + collectNestedTypeNames(f.fields)
            }
        }

        /**
         * Walks nested-builder type trees and collects the [Reference.Custom] names appearing
         * on each nested type's own fields. This ensures the emitted DSL file imports model
         * types referenced only by inlined nested builders (e.g. an enum field on a nested
         * object that doesn't show up in the root body's direct fields).
         */
        private fun collectFieldTypeNames(
            fields: List<BodyFieldShape>,
            types: Map<String, Type>,
        ): List<String> = fields.flatMap { f ->
            when (f) {
                is BodyFieldShape.Primitive -> emptyList()
                is BodyFieldShape.NestedObject -> {
                    val nestedFieldRefs = types[f.typeName]?.shape?.value
                        ?.flatMap { collectCustomNames(it.reference) }
                        ?: emptyList()
                    nestedFieldRefs + collectFieldTypeNames(f.fields, types)
                }
                is BodyFieldShape.NestedList -> {
                    val nestedFieldRefs = types[f.elementTypeName]?.shape?.value
                        ?.flatMap { collectCustomNames(it.reference) }
                        ?: emptyList()
                    nestedFieldRefs + collectFieldTypeNames(f.fields, types)
                }
            }
        }

        private fun extractBodyFields(
            typeName: String,
            types: Map<String, Type>,
            refined: Map<String, Refined>,
            visited: Set<String>,
        ): List<BodyFieldShape> {
            if (typeName in visited) return emptyList()
            val type = types[typeName] ?: return emptyList()
            val nextVisited = visited + typeName
            return type.shape.value.map { field ->
                val name = field.identifier.value
                when (val ref = field.reference) {
                    is Reference.Custom -> if (ref.value in types) {
                        BodyFieldShape.NestedObject(
                            name = name,
                            typeName = ref.value,
                            fields = extractBodyFields(ref.value, types, refined, nextVisited),
                        )
                    } else {
                        BodyFieldShape.Primitive(name, mapWithRefinedUnwrap(ref, refined))
                    }
                    is Reference.Iterable -> {
                        val inner = ref.reference
                        if (inner is Reference.Custom && inner.value in types) {
                            BodyFieldShape.NestedList(
                                name = name,
                                elementTypeName = inner.value,
                                fields = extractBodyFields(inner.value, types, refined, nextVisited),
                            )
                        } else {
                            BodyFieldShape.Primitive(name, mapWithRefinedUnwrap(ref, refined))
                        }
                    }
                    else -> BodyFieldShape.Primitive(name, mapWithRefinedUnwrap(ref, refined))
                }
            }
        }

        /** Like [KotlinTypeMapper.map], but replaces a `Reference.Custom` to a [Refined] with the
         *  refined's underlying primitive type (preserving nullability/iterables/dicts wrapping). */
        internal fun mapWithRefinedUnwrap(reference: Reference, refined: Map<String, Refined>): String = when (reference) {
            is Reference.Custom -> refined[reference.value]?.let { r ->
                KotlinTypeMapper.map(r.reference.copy(isNullable = reference.isNullable))
            } ?: KotlinTypeMapper.map(reference)
            is Reference.Iterable -> {
                val inner = mapWithRefinedUnwrap(reference.reference, refined)
                if (reference.isNullable) "List<$inner>?" else "List<$inner>"
            }
            is Reference.Dict -> {
                val inner = mapWithRefinedUnwrap(reference.reference, refined)
                if (reference.isNullable) "Map<String, $inner>?" else "Map<String, $inner>"
            }
            else -> KotlinTypeMapper.map(reference)
        }
    }
}
