package community.flock.wirespec.integration.kotest

import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext

/**
 * A coroutine-local "current request configuration" for a spec whose single transport applies
 * per-block configuration — authentication, headers — on every call.
 *
 * The wrapper-free `*.call { … }` entry points resolve one [WirespecEndpointContext] per test, so identity
 * cannot be switched by swapping contexts. Instead, hold the active config here and have the
 * transport read [current] on each request; [with] runs a block under a config and restores the outer
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
 * // …later, inside a test:
 * scope.with(AsAgent(agent)) { gateway.postRun { … } }
 * ```
 *
 * The config is carried on the coroutine context (via [asContextElement]) and mirrored onto a
 * [ThreadLocal] so a non-suspending transport hook (e.g. Ktor's `HttpRequestBuilder` block) can read
 * it. Nested [with] blocks restore the enclosing config when they exit.
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
