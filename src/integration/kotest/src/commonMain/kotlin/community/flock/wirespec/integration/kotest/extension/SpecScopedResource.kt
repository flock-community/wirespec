package community.flock.wirespec.integration.kotest.extension

import io.kotest.core.spec.Spec
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * A resource built at most once per [Spec] (lazily, under a mutex so a `suspend` factory runs a
 * single time even under concurrent access), cached for reuse across that spec's tests, and dropped
 * — optionally [close]d if [AutoCloseable] — when the spec ends. Keyed per spec (not per extension
 * instance) so a single extension registered in a Kotest `ProjectConfig` still gives each spec its
 * own resource.
 *
 * Shared by [WirespecChannelExtension] and [WirespecMockExtension], whose managed transportation /
 * server lifecycles are otherwise identical.
 */
internal class SpecScopedResource<T : Any>(
    private val closeOnRemove: Boolean,
    private val factory: suspend (Spec) -> T,
) {
    private val bySpec = ConcurrentHashMap<Spec, T>()
    private val buildLock = Mutex()

    /** The resource for [spec], building (and caching) it on first access. */
    suspend fun get(spec: Spec): T = bySpec[spec] ?: buildLock.withLock {
        bySpec[spec] ?: factory(spec).also { bySpec[spec] = it }
    }

    /** Drop [spec]'s resource, closing it when it is [AutoCloseable] and [closeOnRemove] is set. */
    fun remove(spec: Spec) {
        val removed = bySpec.remove(spec)
        if (closeOnRemove) (removed as? AutoCloseable)?.close()
    }
}
