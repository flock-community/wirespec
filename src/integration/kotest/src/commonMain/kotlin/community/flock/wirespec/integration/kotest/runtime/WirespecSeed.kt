package community.flock.wirespec.integration.kotest.runtime

import io.kotest.property.RandomSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/** Coroutine-context element carrying the per-test [randomSource] every eager wirespec call draws from. */
class WirespecSeed internal constructor(
    val randomSource: RandomSource,
) : AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<WirespecSeed>

    /** Seed of [randomSource], surfaced in failure messages for reproducibility. */
    val seed: Long get() = randomSource.seed
}

private fun newSeededRandomSource(): RandomSource = RandomSource.seeded(System.nanoTime())

/** The [WirespecSeed] already installed for this test, or a fresh seeded one. */
internal fun WirespecSeed?.orNew(): WirespecSeed = this ?: WirespecSeed(newSeededRandomSource())

/** Read the shared [RandomSource] installed by a wirespec extension. */
internal suspend fun currentRandomSource(): RandomSource = coroutineContext[WirespecSeed]?.randomSource ?: error(
    "No wirespec seed in scope. Register a wirespec extension " +
        "(`WirespecEndpointExtension`, `WirespecChannelExtension` or `WirespecMockExtension`) on the spec.",
)

/** Read the shared [seed][WirespecSeed.seed], for reproducibility messages. */
internal suspend fun currentSeed(): Long = coroutineContext[WirespecSeed]?.seed ?: error(
    "No wirespec seed in scope. Register a wirespec extension " +
        "(`WirespecEndpointExtension`, `WirespecChannelExtension` or `WirespecMockExtension`) on the spec.",
)
