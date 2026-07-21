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
    val bodyKind: BodyKind,
    /**
     * Name of the body's element Type — `"Pet"` for both `Object` (body is `Pet`) and `List` (body is `List<Pet>`).
     * `null` for `BodyKind.None`. This is the name to use when constructing builder class names, since it is always
     * a valid Kotlin identifier segment (unlike a `"List<T>"` rendering of a list body).
     */
    val bodyElementType: String?,
    /**
     * Recursive classification of body fields. Each field is one of:
     *  - [BodyFieldShape.Primitive] — leaf, declared as `Arb<KotlinType>?`
     *  - [BodyFieldShape.NestedObject] — drills into a known custom [Type], emits `<field> { … }` overload
     *  - [BodyFieldShape.NestedList] — `Iterable<Custom>` of a known [Type], registers paths with `"*"` segment
     */
    val bodyFieldShapes: List<BodyFieldShape>,
    /** One entry per declared response variant (`200`, `201`, …), used to build random responses. */
    val responseVariants: List<ResponseVariantShape>,
    val modelImports: List<String>,
) {
    data class NamedTypedField(val name: String, val kotlinType: String, val isNullable: Boolean = false)

    /**
     * A single response variant (`Response<status>`), carrying enough to build a random
     * instance: the variant class name, its body kind/type, and its flattened header fields.
     * The body is set as a whole-value `Gen<BodyType>` (defaulting to a generated value); each
     * header field is a whole-value `Gen<HeaderType>` (defaulting to a generated value).
     */
    data class ResponseVariantShape(
        /** Numeric status, e.g. `"201"`. */
        val status: String,
        /** Generated variant class simple name, e.g. `"Response201"`. */
        val className: String,
        val bodyKind: BodyKind,
        /** Full Kotlin type of the body, e.g. `"TodoDto"` or `"List<TodoDto>"`. `null` when there is no body. */
        val bodyType: String?,
        /** Name of the body's element Type — `"TodoDto"` for both object and list bodies. `null` for no body. */
        val bodyElementType: String?,
        val headerFields: List<NamedTypedField>,
    )

    enum class BodyKind { None, Object, List }

    sealed interface BodyFieldShape {
        val name: String

        /**
         * Leaf body field declared as `Gen<[kotlinType]>?` in the typed builder (where
         * [kotlinType] is the refined-unwrapped base primitive). When the field wraps a
         * [Refined] type, [refinedTypeName] names the wrapper class so the generated body
         * transform can re-wrap the drawn primitive — `Refined(v)` for a scalar,
         * `v.map { Refined(it) }` for a list. [isList]/[isNullable] pick the wrap shape.
         */
        data class Primitive(
            override val name: String,
            val kotlinType: String,
            val refinedTypeName: String? = null,
            val isList: Boolean = false,
            val isNullable: Boolean = false,
        ) : BodyFieldShape
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
            val (bodyKind, bodyElementType) = classifyBody(bodyRef)

            // Body-field kotlinType unwraps refined wrappers to their base primitive: the typed
            // body{} builder declares the field as Arb<BaseType>, and the runtime's RefinedWrapper
            // wraps each drawn primitive into the refined class via its single-arg ctor. Without
            // this unwrap, overriding a refined field at runtime throws "expected Arb<BaseType>
            // for refined …, got value of type …".
            val bodyFieldShapes: List<BodyFieldShape> = bodyElementType
                ?.let { extractBodyFields(it, types, refined, visited = emptySet()) }
                ?: emptyList()

            val responseVariants = endpoint.responses
                .mapNotNull { response ->
                    val status = response.status.takeIf { it.all(Char::isDigit) } ?: return@mapNotNull null
                    val respBodyRef = response.content?.reference
                    val respBodyType = respBodyRef?.let { if (it is Reference.Unit) null else KotlinTypeMapper.map(it) }
                    val (respBodyKind, respElementName) = classifyBody(respBodyRef)
                    ResponseVariantShape(
                        status = status,
                        className = "Response$status",
                        bodyKind = respBodyKind,
                        bodyType = respBodyType,
                        bodyElementType = respElementName,
                        headerFields = response.headers.map {
                            NamedTypedField(it.identifier.value, KotlinTypeMapper.map(it.reference), it.reference.isNullable)
                        },
                    )
                }
                // Collapse duplicate statuses (a status can be declared once) keeping the first.
                .distinctBy { it.status }

            val refs = buildList {
                endpoint.path.filterIsInstance<Endpoint.Segment.Param>().forEach { add(it.reference) }
                endpoint.queries.forEach { add(it.reference) }
                endpoint.headers.forEach { add(it.reference) }
                if (bodyRef != null) add(bodyRef)
                endpoint.responses.forEach { response ->
                    response.content?.reference?.let { add(it) }
                    response.headers.forEach { add(it.reference) }
                }
            }
            val bodyFieldRefs = bodyElementType
                ?.let { types[it] }
                ?.shape?.value
                ?.map { it.reference }
                ?: emptyList()
            val modelImports = modelImportsFor(refs + bodyFieldRefs, bodyFieldShapes, types)

            return EndpointShape(
                name = endpoint.identifier.value,
                pathFields = pathFields,
                queryFields = queryFields,
                headerFields = headerFields,
                bodyKind = bodyKind,
                bodyElementType = bodyElementType,
                bodyFieldShapes = bodyFieldShapes,
                responseVariants = responseVariants,
                modelImports = modelImports,
            )
        }

        /** Classifies a request/response body reference into its [BodyKind] and element type name. */
        private fun classifyBody(reference: Reference?): Pair<BodyKind, String?> = when (reference) {
            null, is Reference.Unit -> BodyKind.None to null
            is Reference.Custom -> BodyKind.Object to reference.value
            is Reference.Iterable -> (reference.reference as? Reference.Custom)
                ?.let { BodyKind.List to it.value }
                ?: (BodyKind.None to null)
            else -> BodyKind.None to null
        }

        /**
         * The distinct model type names an operation's DSL file must import: the [Reference.Custom]
         * names reachable from its top-level [refs], plus the nested-type and field-type names
         * discovered while walking its body/payload [fieldShapes].
         */
        internal fun modelImportsFor(
            refs: List<Reference>,
            fieldShapes: List<BodyFieldShape>,
            types: Map<String, Type>,
        ): List<String> = (
            refs.flatMap(::collectCustomNames) +
                collectNestedTypeNames(fieldShapes) +
                collectFieldTypeNames(fieldShapes, types)
            ).distinct()

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

        internal fun extractBodyFields(
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
                        primitiveOf(name, ref, refined)
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
                            primitiveOf(name, ref, refined)
                        }
                    }
                    else -> primitiveOf(name, ref, refined)
                }
            }
        }

        /**
         * Build a [BodyFieldShape.Primitive], recording the [Refined] wrapper class (if any)
         * so the generated body transform can re-wrap the drawn base primitive. A scalar
         * `Refined` field carries `isList = false`; an `Iterable<Refined>` carries
         * `isList = true`. Non-refined fields get `refinedTypeName = null`.
         */
        private fun primitiveOf(
            name: String,
            ref: Reference,
            refined: Map<String, Refined>,
        ): BodyFieldShape.Primitive {
            val (refinedTypeName, isList) = when {
                ref is Reference.Custom && ref.value in refined -> ref.value to false
                ref is Reference.Iterable -> {
                    val inner = ref.reference
                    if (inner is Reference.Custom && inner.value in refined) inner.value to true else null to false
                }
                else -> null to false
            }
            return BodyFieldShape.Primitive(
                name = name,
                kotlinType = mapWithRefinedUnwrap(ref, refined),
                refinedTypeName = refinedTypeName,
                isList = isList,
                isNullable = ref.isNullable,
            )
        }

        /** Like [KotlinTypeMapper.map], but replaces a `Reference.Custom` to a [Refined] with the
         *  refined's underlying primitive type (preserving nullability/iterables/dicts wrapping). */
        private fun mapWithRefinedUnwrap(reference: Reference, refined: Map<String, Refined>): String = when (reference) {
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
