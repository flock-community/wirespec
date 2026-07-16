package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type

/**
 * The Kotest-DSL view of a standalone record [Type]: its name, the [EndpointShape.BodyFieldShape]
 * classification of its fields (reused from the endpoint body machinery), and its model imports.
 */
internal data class TypeShape(
    val name: String,
    val fields: List<EndpointShape.BodyFieldShape>,
    val modelImports: List<String>,
) {
    companion object {
        fun from(
            type: Type,
            types: Map<String, Type> = emptyMap(),
            refined: Map<String, Refined> = emptyMap(),
        ): TypeShape {
            val name = type.identifier.value
            val fields = EndpointShape.extractBodyFields(name, types, refined, visited = emptySet())
            val directRefs = type.shape.value.map { it.reference }
            val modelImports = (
                directRefs.flatMap(EndpointShape::collectCustomNames) +
                    EndpointShape.collectNestedTypeNames(fields) +
                    EndpointShape.collectFieldTypeNames(fields, types)
                ).distinct()
            return TypeShape(name = name, fields = fields, modelImports = modelImports)
        }
    }
}
