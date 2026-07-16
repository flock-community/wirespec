package community.flock.wirespec.integration.kotest

import community.flock.wirespec.integration.kotest.runtime.WirespecAmbient
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import io.kotest.property.RandomSource
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

// NB: kotest 6.x relocated TestResult to io.kotest.engine.test; io.kotest.core.test.TestResult no longer exists.

/**
 * Installs the channel half of the ambient wirespec context around every test, so
 * wrapper-free `SomeChannel.generate.message { … }.send()` and
 * `SomeChannel.generate.listen { expecting { … } }` calls resolve their transport and a
 * per-test [RandomSource].
 *
 * Supply the transport eagerly, or let this extension build and own it:
 *
 * ```
 * // eager — you own the transport's lifecycle
 * extension(WirespecChannelExtension(myChannelContext))
 * extension(WirespecChannelExtension(myTransport, mySerialization))
 *
 * // managed — built once (lazily) from `suspend` factories, reset per test, closed after the spec.
 * // The factories carry any framework wiring (e.g. Spring via `testContextManager()`), so this
 * // extension itself stays framework-agnostic.
 * extension(
 *     WirespecChannelExtension(
 *         serialization = { myWirespecSerialization() },
 *         transport = { KafkaChannelTransport(bootstrapServers(), TOPIC) },
 *         reset = { it.clear() },
 *     ),
 * )
 * ```
 *
 * Composes with [WirespecEndpointExtension]: when both are registered they merge into a
 * single ambient (sharing one per-test [RandomSource]), so a spec can drive endpoints and
 * channels together.
 */
class WirespecChannelExtension internal constructor(
    private val eager: WirespecChannelContext?,
    private val serializationFactory: (suspend () -> Wirespec.Serialization)?,
    private val transportFactory: (suspend () -> ChannelTransport)?,
    private val defaultTopic: String?,
    private val reset: (ChannelTransport) -> Unit,
) : TestCaseExtension,
    AfterSpecListener {

    constructor(channel: WirespecChannelContext) : this(channel, null, null, null, {})

    /** Convenience: build the [WirespecChannelContext] from a [transport] + [serialization] directly. */
    constructor(
        transport: ChannelTransport,
        serialization: Wirespec.Serialization,
        defaultTopic: String? = null,
    ) : this(WirespecChannelContext(transport, serialization, defaultTopic))

    // Built once and reused across the spec's tests (managed mode only). Kotest runs a spec's tests
    // sequentially by default, so a plain field is enough (no cross-test concurrency to guard).
    private var created: ChannelTransport? = null

    override suspend fun intercept(
        testCase: TestCase,
        execute: suspend (TestCase) -> TestResult,
    ): TestResult {
        val channel = eager ?: run {
            val transport = created ?: transportFactory!!().also { created = it }
            reset(transport)
            WirespecChannelContext(transport, serializationFactory!!(), defaultTopic)
        }
        val ambient = coroutineContext[WirespecAmbient]?.withChannel(channel)
            ?: WirespecAmbient(endpoint = null, channel = channel, randomSource = RandomSource.seeded(System.nanoTime()))
        return withContext(ambient) { execute(testCase) }
    }

    override suspend fun afterSpec(spec: Spec) {
        // Only a managed transport is owned here; an eager one belongs to the caller.
        (created as? AutoCloseable)?.close()
    }
}

/**
 * Managed [WirespecChannelExtension]: builds the transport once (lazily) via a `suspend` factory,
 * resets it before each test, and closes it after the spec. The generic [T] lets [reset] receive
 * the concrete transport type (so `reset = { it.clear() }` needs no cast). Named arguments
 * (`serialization = …`, `transport = …`) select this factory over the eager constructors.
 */
fun <T : ChannelTransport> WirespecChannelExtension(
    serialization: suspend () -> Wirespec.Serialization,
    transport: suspend () -> T,
    defaultTopic: String? = null,
    reset: (T) -> Unit = {},
): WirespecChannelExtension = WirespecChannelExtension(
    eager = null,
    serializationFactory = serialization,
    transportFactory = transport,
    defaultTopic = defaultTopic,
    reset = {
        @Suppress("UNCHECKED_CAST")
        reset(it as T)
    },
)
