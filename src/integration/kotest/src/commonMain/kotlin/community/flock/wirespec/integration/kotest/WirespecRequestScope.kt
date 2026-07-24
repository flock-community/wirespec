package community.flock.wirespec.integration.kotest

import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext

/** A coroutine-local "current request configuration" (auth, headers) that [with] scopes per block. */
class WirespecRequestScope<C> {
    private val local = ThreadLocal<C?>()

    /** The config active for the current block, or `null` outside any [with]. */
    fun current(): C? = local.get()

    /** Run [block] with [config] active; the previous config is restored when [block] returns. */
    suspend fun <T> with(
        config: C,
        block: suspend () -> T,
    ): T = withContext(local.asContextElement(config)) { block() }
}
