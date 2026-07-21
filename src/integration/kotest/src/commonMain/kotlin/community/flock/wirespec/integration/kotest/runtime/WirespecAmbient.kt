package community.flock.wirespec.integration.kotest.runtime

import community.flock.wirespec.integration.kotest.context.WirespecChannelContext
import community.flock.wirespec.integration.kotest.context.WirespecEndpointContext
import community.flock.wirespec.integration.kotest.context.WirespecMockContext
import io.kotest.property.RandomSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Coroutine-context element carrying everything an eager wirespec call needs: the
 * per-test [randomSource] (its [seed] is printed on failure for reproducibility)
 * and the endpoint / channel transport contexts installed by
 * [WirespecEndpointExtension][community.flock.wirespec.integration.kotest.extension.WirespecEndpointExtension]
 * and [WirespecChannelExtension][community.flock.wirespec.integration.kotest.extension.WirespecChannelExtension].
 *
 * A context is optional: an endpoint-only spec registers just the endpoint extension
 * (and vice versa), and the corresponding accessor raises a clear error only if a call
 * actually needs the missing transport. Registering several extensions merges their
 * contexts into one ambient via [withEndpoint] / [withChannel] / [withMock], sharing a
 * single per-test [randomSource].
 */
class WirespecAmbient internal constructor(
    private val endpoint: WirespecEndpointContext?,
    private val channel: WirespecChannelContext?,
    private val mock: WirespecMockContext?,
    val randomSource: RandomSource,
) : AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<WirespecAmbient>

    /** Seed of [randomSource], surfaced in failure messages for reproducibility. */
    val seed: Long get() = randomSource.seed

    /** Copy carrying [endpoint], keeping the existing channel/mock context and [randomSource]. */
    internal fun withEndpoint(endpoint: WirespecEndpointContext): WirespecAmbient = WirespecAmbient(endpoint, channel, mock, randomSource)

    /** Copy carrying [channel], keeping the existing endpoint/mock context and [randomSource]. */
    internal fun withChannel(channel: WirespecChannelContext): WirespecAmbient = WirespecAmbient(endpoint, channel, mock, randomSource)

    /** Copy carrying [mock], keeping the existing endpoint/channel context and [randomSource]. */
    internal fun withMock(mock: WirespecMockContext): WirespecAmbient = WirespecAmbient(endpoint, channel, mock, randomSource)

    fun endpointContext(): WirespecEndpointContext = endpoint ?: error(
        "No WirespecEndpointContext configured. Register " +
            "`WirespecEndpointExtension(endpoint)` on the spec.",
    )

    fun channelContext(): WirespecChannelContext = channel ?: error(
        "No WirespecChannelContext configured. Register " +
            "`WirespecChannelExtension(channel)` on the spec.",
    )

    fun mockContext(): WirespecMockContext = mock ?: error(
        "No WirespecMockContext configured. Register " +
            "`WirespecMockExtension(mock)` on the spec.",
    )
}

/** Read the ambient element installed by the wirespec extension(s). */
internal suspend fun currentAmbient(): WirespecAmbient = coroutineContext[WirespecAmbient] ?: error(
    "No wirespec ambient context in scope. Register the extension with " +
        "`extension(WirespecEndpointExtension(…))` or `extension(WirespecChannelExtension(…))` on the spec.",
)

// A fresh per-test [RandomSource]; its seed is printed on failure so a run can be reproduced. The
// three extensions share this one source of seeding, rather than each calling RandomSource.seeded.
private fun newAmbientRandomSource(): RandomSource = RandomSource.seeded(System.nanoTime())

/** Merge [endpoint] into the ambient already in scope for this test, or start a fresh seeded one. */
internal fun WirespecAmbient?.mergeEndpoint(endpoint: WirespecEndpointContext): WirespecAmbient = this?.withEndpoint(endpoint) ?: WirespecAmbient(endpoint = endpoint, channel = null, mock = null, randomSource = newAmbientRandomSource())

/** Merge [channel] into the ambient already in scope for this test, or start a fresh seeded one. */
internal fun WirespecAmbient?.mergeChannel(channel: WirespecChannelContext): WirespecAmbient = this?.withChannel(channel) ?: WirespecAmbient(endpoint = null, channel = channel, mock = null, randomSource = newAmbientRandomSource())

/** Merge [mock] into the ambient already in scope for this test, or start a fresh seeded one. */
internal fun WirespecAmbient?.mergeMock(mock: WirespecMockContext): WirespecAmbient = this?.withMock(mock) ?: WirespecAmbient(endpoint = null, channel = null, mock = mock, randomSource = newAmbientRandomSource())
