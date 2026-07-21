package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.integration.kotest.context.ChannelTransport
import community.flock.wirespec.integration.kotest.context.WirespecChannelContext
import community.flock.wirespec.integration.kotest.runtime.WirespecAmbient
import community.flock.wirespec.integration.kotest.runtime.mergeChannel
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

// NB: kotest 6.x relocated TestResult to io.kotest.engine.test; io.kotest.core.test.TestResult no longer exists.

/**
 * Installs the channel half of the ambient wirespec context around every test, so
 * wrapper-free `SomeChannel.generate.message { … }.send()` calls resolve their transportation and a
 * per-test [RandomSource]. Asserting on what the app published is left to the test's own broker
 * consumer.
 *
 * Supply the transportation eagerly, or let this extension build and own it:
 *
 * ```
 * // eager — you own the transportation's lifecycle
 * extension(WirespecChannelExtension(myChannelContext))
 * extension(WirespecChannelExtension(myTransportation, mySerialization))
 *
 * // managed — built once per spec (lazily) from `suspend` factories, reset per test, closed after
 * // the spec. The factories carry any framework wiring (e.g. Spring via `testContextManager()`), so
 * // this extension itself stays framework-agnostic. Building per spec (not per instance) makes a
 * // single instance safe to register once in a Kotest `ProjectConfig` for the whole suite.
 * extension(
 *     WirespecChannelExtension(
 *         serialization = { myWirespecSerialization() },
 *         transportation = { KafkaChannelTransport(bootstrapServers()) },
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
    private val transportationFactory: (suspend () -> ChannelTransport)?,
    private val reset: (ChannelTransport) -> Unit,
) : TestCaseExtension,
    AfterSpecListener {

    constructor(channel: WirespecChannelContext) : this(channel, null, null, {})

    /** Convenience: build the [WirespecChannelContext] from a [transportation] + [serialization] directly. */
    constructor(
        transportation: ChannelTransport,
        serialization: Wirespec.Serialization,
    ) : this(WirespecChannelContext(transportation, serialization))

    // Managed mode builds one transportation per spec, resolved (and closed) via SpecScopedResource, so a
    // single instance registered in a ProjectConfig serves every spec correctly — each spec gets its own
    // transportation (e.g. its own broker). An eager transportation bypasses this and belongs to the caller.
    private val transportations = SpecScopedResource<ChannelTransport>(closeOnRemove = true) { transportationFactory!!() }

    override suspend fun intercept(
        testCase: TestCase,
        execute: suspend (TestCase) -> TestResult,
    ): TestResult {
        val channel = eager ?: run {
            val transportation = transportations.get(testCase.spec)
            reset(transportation)
            WirespecChannelContext(transportation, serializationFactory!!())
        }
        val ambient = coroutineContext[WirespecAmbient].mergeChannel(channel)
        return withContext(ambient) { execute(testCase) }
    }

    override suspend fun afterSpec(spec: Spec) = transportations.remove(spec)
}

/**
 * Managed [WirespecChannelExtension]: builds the transportation once per spec (lazily) via a `suspend`
 * factory, resets it before each test, and closes it after the spec. The generic [T] lets [reset]
 * receive the concrete transportation type (so `reset = { it.clear() }` needs no cast). Named arguments
 * (`serialization = …`, `transportation = …`) select this factory over the eager constructors.
 */
fun <T : ChannelTransport> WirespecChannelExtension(
    serialization: suspend () -> Wirespec.Serialization,
    transportation: suspend () -> T,
    reset: (T) -> Unit = {},
): WirespecChannelExtension = WirespecChannelExtension(
    eager = null,
    serializationFactory = serialization,
    transportationFactory = transportation,
    reset = {
        @Suppress("UNCHECKED_CAST")
        reset(it as T)
    },
)
