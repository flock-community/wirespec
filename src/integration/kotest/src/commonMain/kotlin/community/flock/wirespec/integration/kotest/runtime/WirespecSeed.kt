package community.flock.wirespec.integration.kotest.runtime

import io.kotest.property.RandomSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

internal class WirespecSeed internal constructor(
    val randomSource: RandomSource,
) : AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<WirespecSeed>

    val seed: Long get() = randomSource.seed
}

private fun newSeededRandomSource(): RandomSource = RandomSource.seeded(System.nanoTime())

internal fun WirespecSeed?.orNew(): WirespecSeed = this ?: WirespecSeed(newSeededRandomSource())

internal suspend fun currentRandomSource(): RandomSource = coroutineContext[WirespecSeed]?.randomSource ?: error(
    "No wirespec seed in scope. Register a wirespec extension " +
        "(`WirespecEndpointExtension`, `WirespecChannelExtension` or `WirespecMockExtension`) on the spec.",
)

internal suspend fun currentSeed(): Long = coroutineContext[WirespecSeed]?.seed ?: error(
    "No wirespec seed in scope. Register a wirespec extension " +
        "(`WirespecEndpointExtension`, `WirespecChannelExtension` or `WirespecMockExtension`) on the spec.",
)
