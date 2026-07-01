package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type

internal data class ChannelShape(
    val name: String,
    val payloadType: String,
    val payloadFields: List<EndpointShape.NamedTypedField>,
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
            // Payload-field kotlinType unwraps refined wrappers to their base primitive — same
            // reason as EndpointShape.bodyFields (the runtime expects Arb<BaseType> for refined).
            val payloadFields = (payloadRef as? Reference.Custom)
                ?.let { types[it.value] }
                ?.shape?.value
                ?.map { EndpointShape.NamedTypedField(it.identifier.value, EndpointShape.mapWithRefinedUnwrap(it.reference, refined)) }
                ?: emptyList()

            val payloadFieldRefs = (payloadRef as? Reference.Custom)
                ?.let { types[it.value] }
                ?.shape?.value
                ?.map { it.reference }
                ?: emptyList()
            val modelImports = (listOf(payloadRef) + payloadFieldRefs).flatMap(::collectCustomNames).distinct()

            return ChannelShape(
                name = channel.identifier.value,
                payloadType = payloadType,
                payloadFields = payloadFields,
                modelImports = modelImports,
            )
        }

        private fun collectCustomNames(reference: Reference): List<String> = when (reference) {
            is Reference.Custom -> listOf(reference.value)
            is Reference.Iterable -> collectCustomNames(reference.reference)
            is Reference.Dict -> collectCustomNames(reference.reference)
            else -> emptyList()
        }
    }
}
