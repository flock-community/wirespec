package community.flock.wirespec.examples.kotest.scenario

import community.flock.wirespec.integration.kotest.WirespecChannelContext
import community.flock.wirespec.integration.kotest.WirespecTestContext
import community.flock.wirespec.integration.kotest.context.ContextProvider
import io.kotest.core.spec.Spec

/**
 * Bridges the kotest scenario-DSL runtime to a running [ScenarioContext] spec.
 * Discovered via `META-INF/services`; returns `null` for specs that don't expose a
 * scenario context, so the runtime tries the next provider.
 */
class ScenarioContextProvider : ContextProvider {
    override fun endpointContext(spec: Spec): WirespecTestContext? =
        (spec as? ScenarioContext)?.endpointContext()

    override fun channelContext(spec: Spec): WirespecChannelContext? =
        (spec as? ScenarioContext)?.channelContext()
}
