package community.flock.wirespec.examples.kotest.scenario

import community.flock.wirespec.integration.kotest.WirespecChannelContext
import community.flock.wirespec.integration.kotest.WirespecTestContext

/**
 * Implemented by the base scenario spec so the [ScenarioContextProvider] can hand
 * the relevant transport context to the auto-resolving `*.call { … }` DSL.
 */
interface ScenarioContext {
    fun endpointContext(): WirespecTestContext
    fun channelContext(): WirespecChannelContext
}
