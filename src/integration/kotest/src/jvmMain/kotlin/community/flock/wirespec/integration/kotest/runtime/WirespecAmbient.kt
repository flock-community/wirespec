package community.flock.wirespec.integration.kotest.runtime

import community.flock.wirespec.integration.kotest.WirespecChannelContext
import community.flock.wirespec.integration.kotest.WirespecTestContext
import community.flock.wirespec.integration.kotest.context.ContextRegistry
import io.kotest.core.spec.Spec
import io.kotest.property.RandomSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Coroutine-context element carrying everything an eager wirespec call needs: the
 * per-test [randomSource] (its [seed] is printed on failure for reproducibility)
 * and lazily-resolved endpoint / channel transport contexts.
 *
 * Each context is resolved on first use via the [ContextRegistry] SPI from the
 * running [spec], which avoids any extension-ordering dependency with e.g.
 * Spring's lifecycle extension.
 */
class WirespecAmbient internal constructor(
    private val spec: Spec?,
    val randomSource: RandomSource,
) : AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<WirespecAmbient>

    /** Seed of [randomSource], surfaced in failure messages for reproducibility. */
    val seed: Long get() = randomSource.seed

    private val endpoint: WirespecTestContext? by lazy {
        spec?.let { s -> ContextRegistry.providers.firstNotNullOfOrNull { it.endpointContext(s) } }
    }

    private val channel: WirespecChannelContext? by lazy {
        spec?.let { s -> ContextRegistry.providers.firstNotNullOfOrNull { it.channelContext(s) } }
    }

    fun endpointContext(): WirespecTestContext = endpoint ?: error(
        "No WirespecTestContext available" + (spec?.let { " for ${it::class.simpleName}" } ?: "") + ". " +
            "Register a ContextProvider via META-INF/services or " +
            "`@ApplyExtension(WirespecExtension::class)` on the spec.",
    )

    fun channelContext(): WirespecChannelContext = channel ?: error(
        "No WirespecChannelContext available" + (spec?.let { " for ${it::class.simpleName}" } ?: "") + ". " +
            "Register a ContextProvider (with channelContext) via META-INF/services or " +
            "`@ApplyExtension(WirespecExtension::class)` on the spec.",
    )
}

/** Read the ambient element installed by `WirespecExtension`. */
internal suspend fun currentAmbient(): WirespecAmbient = coroutineContext[WirespecAmbient] ?: error(
    "No wirespec ambient context in scope. Mount @ApplyExtension(WirespecExtension::class) on the spec.",
)
