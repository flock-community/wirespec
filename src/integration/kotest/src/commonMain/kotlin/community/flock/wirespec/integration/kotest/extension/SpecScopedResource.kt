package community.flock.wirespec.integration.kotest.extension

import io.kotest.core.spec.Spec
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal class SpecScopedResource<T : Any>(
    private val closeOnRemove: Boolean,
    private val factory: suspend (Spec) -> T,
) {
    private val bySpec = ConcurrentHashMap<Spec, T>()
    private val buildLock = Mutex()

    suspend fun get(spec: Spec): T = bySpec[spec] ?: buildLock.withLock {
        bySpec[spec] ?: factory(spec).also { bySpec[spec] = it }
    }

    fun remove(spec: Spec) {
        val removed = bySpec.remove(spec)
        if (closeOnRemove) (removed as? AutoCloseable)?.close()
    }
}
