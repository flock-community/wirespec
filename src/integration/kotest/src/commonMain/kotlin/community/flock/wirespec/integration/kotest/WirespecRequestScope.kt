package community.flock.wirespec.integration.kotest

import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext

/**
 * A coroutine-local "current request configuration" (auth, headers) for a spec whose single transport
 * reads [current] on each call. Since `*.call { … }` resolves one context per test, identity can't be
 * switched by swapping contexts — instead [with] runs a block under a config and restores the outer
 * one on exit. [C] is the app's own config/identity type.
 *
 * ```
 * val scope = WirespecRequestScope<Identity>()
 * val transportation = KtorTransportation(client) {
 *     when (val id = scope.current()) {
 *         is AsUser -> bearerAuth(id.token)
 *         is AsAgent -> basicAuth(id.name, id.secret)
 *         null -> error("No active identity — wrap calls in scope.with(…) { }")
 *     }
 * }
 * scope.with(AsAgent(agent)) { gateway.postRun { … } }
 * ```
 *
 * The config is mirrored onto a [ThreadLocal] (as well as the coroutine context) so a non-suspending
 * transport hook (e.g. Ktor's `HttpRequestBuilder` block) can read it.
 */
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
