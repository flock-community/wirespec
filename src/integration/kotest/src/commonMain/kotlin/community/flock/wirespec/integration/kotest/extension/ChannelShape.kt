package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type

internal data class ChannelShape(
    val name: String,
    val payloadType: String,
    /** Payload record fields (reused from the endpoint body machinery); empty for a primitive payload. */
    val payloadFieldShapes: List<EndpointShape.BodyFieldShape>,
    val modelImports: List<String>,
) {
    companion object {
        fun from(
            channel: Channel,
            types: Map<String, Type> = emptyMap(),
            refined: Map<String, Refined> = emptyMap(),
        ): ChannelShape {
            val payloadRef = channel.reference
            val payloadType = KotlinTypeMapper.map(payloadRef)
            val payloadCustom = payloadRef as? Reference.Custom

            val payloadFieldShapes = payloadCustom
                ?.let { EndpointShape.extractBodyFields(it.value, types, refined, visited = emptySet()) }
                ?: emptyList()

            val directRefs = payloadCustom
                ?.let { types[it.value] }
                ?.shape?.value
                ?.map { it.reference }
                ?: emptyList()
            val modelImports = (
                (listOf(payloadRef) + directRefs).flatMap(EndpointShape::collectCustomNames) +
                    EndpointShape.collectNestedTypeNames(payloadFieldShapes) +
                    EndpointShape.collectFieldTypeNames(payloadFieldShapes, types)
                ).distinct()

            return ChannelShape(
                name = channel.identifier.value,
                payloadType = payloadType,
                payloadFieldShapes = payloadFieldShapes,
                modelImports = modelImports,
            )
        }
    }
}
