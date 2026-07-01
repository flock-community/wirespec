package community.flock.wirespec.integration.kotest.context

import community.flock.wirespec.integration.kotest.WirespecChannelContext
import community.flock.wirespec.integration.kotest.WirespecTestContext
import io.kotest.core.spec.Spec
import java.util.concurrent.ConcurrentHashMap

/**
 * Built-in per-[Spec] store for the transport contexts the wrapper-free `*.call { … }` entry points
 * resolve. It exists so an app whose transport is created per test (a fresh server/client per test)
 * does not have to write its own [ContextProvider] plus a `META-INF/services` file: register the
 * running spec's context here and the shipped [RegistryContextProvider] hands it back.
 *
 * Typical wiring from a spec's own extension:
 *
 * ```
 * override suspend fun intercept(testCase, execute): TestResult {
 *     val ctx = WirespecTestContext(transportation, serialization)
 *     WirespecContextRegistry.registerEndpoint(testCase.spec, ctx)
 *     try {
 *         return WirespecExtension().intercept(testCase, execute)
 *     } finally {
 *         WirespecContextRegistry.unregister(testCase.spec)
 *     }
 * }
 * ```
 *
 * Contexts are keyed on the [Spec] instance (kotest resolves the endpoint context once per test from
 * the running spec), so [unregister] must run after the test to avoid leaking spec instances.
 */
object WirespecContextRegistry {
    private val endpoints = ConcurrentHashMap<Spec, WirespecTestContext>()
    private val channels = ConcurrentHashMap<Spec, WirespecChannelContext>()

    /** Register the endpoint transport [context] for [spec]; resolved by wrapper-free endpoint calls. */
    fun registerEndpoint(
        spec: Spec,
        context: WirespecTestContext,
    ) {
        endpoints[spec] = context
    }

    /** Register the channel transport [context] for [spec]; resolved by wrapper-free channel calls. */
    fun registerChannel(
        spec: Spec,
        context: WirespecChannelContext,
    ) {
        channels[spec] = context
    }

    /** Drop both contexts for [spec]. Call once the test finishes so the spec instance isn't retained. */
    fun unregister(spec: Spec) {
        endpoints.remove(spec)
        channels.remove(spec)
    }

    internal fun endpoint(spec: Spec): WirespecTestContext? = endpoints[spec]

    internal fun channel(spec: Spec): WirespecChannelContext? = channels[spec]
}

/**
 * Ships as the default [ContextProvider] (registered via `META-INF/services`), resolving whatever the
 * running spec registered in [WirespecContextRegistry]. Returns `null` when nothing is registered, so
 * specs that supply their context another way are unaffected — the next provider is tried.
 */
class RegistryContextProvider : ContextProvider {
    override fun endpointContext(spec: Spec): WirespecTestContext? = WirespecContextRegistry.endpoint(spec)

    override fun channelContext(spec: Spec): WirespecChannelContext? = WirespecContextRegistry.channel(spec)
}
