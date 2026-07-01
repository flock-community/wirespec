package community.flock.wirespec.examples.kotest.support

import community.flock.wirespec.integration.kotest.WirespecChannelContext
import community.flock.wirespec.integration.kotest.WirespecTestContext
import community.flock.wirespec.integration.kotest.context.ContextProvider
import io.kotest.core.spec.Spec

/**
 * Supplies the generated `*.call { … }` DSL with the transport contexts from the shared
 * [CampaignTestEnvironment]. Discovered via `META-INF/services`; the spec is irrelevant
 * here because the environment is process-wide, so any spec mounting
 * `@ApplyExtension(WirespecExtension::class)` resolves the same contexts.
 */
class ScenarioContextProvider : ContextProvider {
    override fun endpointContext(spec: Spec): WirespecTestContext = CampaignTestEnvironment.endpointContext
    override fun channelContext(spec: Spec): WirespecChannelContext = CampaignTestEnvironment.channelContext
}
