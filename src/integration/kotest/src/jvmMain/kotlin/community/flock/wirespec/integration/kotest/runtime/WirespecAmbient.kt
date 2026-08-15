package community.flock.wirespec.integration.kotest.runtime

import community.flock.wirespec.integration.kotest.WirespecChannelContext
import community.flock.wirespec.integration.kotest.WirespecTestContext
import io.kotest.property.RandomSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Coroutine-context element carrying everything an eager wirespec call needs: the
 * per-test [randomSource] (its [seed] is printed on failure for reproducibility)
 * and the endpoint / channel transport contexts the spec configured on
 * [WirespecExtension][community.flock.wirespec.integration.kotest.WirespecExtension].
 *
 * A context is optional: an endpoint-only spec leaves [channel] null (and vice
 * versa), and the corresponding accessor raises a clear error only if a call
 * actually needs the missing transport.
 */
class WirespecAmbient internal constructor(
    private val endpoint: WirespecTestContext?,
    private val channel: WirespecChannelContext?,
    val randomSource: RandomSource,
) : AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<WirespecAmbient>

    /** Seed of [randomSource], surfaced in failure messages for reproducibility. */
    val seed: Long get() = randomSource.seed

    fun endpointContext(): WirespecTestContext = endpoint ?: error(
        "No WirespecTestContext configured. Pass one to " +
            "`WirespecExtension(endpoint = …)` on the spec.",
    )

    fun channelContext(): WirespecChannelContext = channel ?: error(
        "No WirespecChannelContext configured. Pass one to " +
            "`WirespecExtension(channel = …)` on the spec.",
    )
}

/** Read the ambient element installed by `WirespecExtension`. */
internal suspend fun currentAmbient(): WirespecAmbient = coroutineContext[WirespecAmbient] ?: error(
    "No wirespec ambient context in scope. Register the extension with " +
        "`extension(WirespecExtension(…))` on the spec.",
)
