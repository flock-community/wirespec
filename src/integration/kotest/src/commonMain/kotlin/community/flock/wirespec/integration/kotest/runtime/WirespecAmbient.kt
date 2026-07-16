package community.flock.wirespec.integration.kotest.runtime

import community.flock.wirespec.integration.kotest.WirespecChannelContext
import community.flock.wirespec.integration.kotest.WirespecEndpointContext
import io.kotest.property.RandomSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Coroutine-context element carrying everything an eager wirespec call needs: the
 * per-test [randomSource] (its [seed] is printed on failure for reproducibility)
 * and the endpoint / channel transport contexts installed by
 * [WirespecEndpointExtension][community.flock.wirespec.integration.kotest.WirespecEndpointExtension]
 * and [WirespecChannelExtension][community.flock.wirespec.integration.kotest.WirespecChannelExtension].
 *
 * A context is optional: an endpoint-only spec registers just the endpoint extension
 * (and vice versa), and the corresponding accessor raises a clear error only if a call
 * actually needs the missing transport. Registering both extensions merges their
 * contexts into one ambient via [withEndpoint] / [withChannel], sharing a single
 * per-test [randomSource].
 */
class WirespecAmbient internal constructor(
    private val endpoint: WirespecEndpointContext?,
    private val channel: WirespecChannelContext?,
    val randomSource: RandomSource,
) : AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<WirespecAmbient>

    /** Seed of [randomSource], surfaced in failure messages for reproducibility. */
    val seed: Long get() = randomSource.seed

    /** Copy carrying [endpoint], keeping the existing channel context and [randomSource]. */
    internal fun withEndpoint(endpoint: WirespecEndpointContext): WirespecAmbient = WirespecAmbient(endpoint, channel, randomSource)

    /** Copy carrying [channel], keeping the existing endpoint context and [randomSource]. */
    internal fun withChannel(channel: WirespecChannelContext): WirespecAmbient = WirespecAmbient(endpoint, channel, randomSource)

    fun endpointContext(): WirespecEndpointContext = endpoint ?: error(
        "No WirespecEndpointContext configured. Register " +
            "`WirespecEndpointExtension(endpoint)` on the spec.",
    )

    fun channelContext(): WirespecChannelContext = channel ?: error(
        "No WirespecChannelContext configured. Register " +
            "`WirespecChannelExtension(channel)` on the spec.",
    )
}

/** Read the ambient element installed by the wirespec extension(s). */
internal suspend fun currentAmbient(): WirespecAmbient = coroutineContext[WirespecAmbient] ?: error(
    "No wirespec ambient context in scope. Register the extension with " +
        "`extension(WirespecEndpointExtension(…))` or `extension(WirespecChannelExtension(…))` on the spec.",
)
