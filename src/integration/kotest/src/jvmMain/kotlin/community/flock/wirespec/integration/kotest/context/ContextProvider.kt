package community.flock.wirespec.integration.kotest.context

import community.flock.wirespec.integration.kotest.WirespecChannelContext
import community.flock.wirespec.integration.kotest.WirespecTestContext
import io.kotest.core.spec.Spec
import java.util.ServiceLoader

/**
 * Service-loader-discovered hook that supplies a framework-specific transport
 * context to the auto-resolving `*.call { … }` entry points: a
 * [WirespecTestContext] for endpoints and a [WirespecChannelContext] for channels.
 *
 * A provider registers itself via
 * `META-INF/services/community.flock.wirespec.integration.kotest.context.ContextProvider`.
 * Both methods default to `null`, so a provider that can't supply a context for a
 * given spec doesn't force callers to change — the next provider is tried.
 */
interface ContextProvider {
    /**
     * Resolve a default [WirespecTestContext] from the running spec instance, or
     * `null` if this provider can't supply one (e.g. no transport configured for
     * this spec); the caller then tries the next provider or surfaces a clear error.
     */
    fun endpointContext(spec: Spec): WirespecTestContext? = null

    /**
     * Resolve a default [WirespecChannelContext] from the running spec instance, or
     * `null` if this provider can't supply one (e.g. no message broker configured
     * for this spec); the caller then tries the next provider.
     */
    fun channelContext(spec: Spec): WirespecChannelContext? = null
}

/**
 * Lazy snapshot of [ContextProvider]s discovered via `ServiceLoader` at first use.
 * Lookup follows classpath order; the first non-null context result wins.
 */
internal object ContextRegistry {
    val providers: List<ContextProvider> by lazy {
        ServiceLoader.load(ContextProvider::class.java, ContextProvider::class.java.classLoader).toList()
    }
}
