package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.integration.kotest.runtime.WirespecSeed
import community.flock.wirespec.integration.kotest.runtime.orNew
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/** Installs the channel half of the ambient wirespec context around every test. */
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
        val seed = coroutineContext[WirespecSeed].orNew()
        return withContext(channel + seed) { execute(testCase) }
    }

    override suspend fun afterSpec(spec: Spec) = transportations.remove(spec)
}

/** Managed [WirespecChannelExtension] that builds the transportation once per spec from `suspend` factories. */
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

/** Framework-neutral broker handle [WirespecChannelExtension] installs and the channel scenario DSL consumes. */
class WirespecChannelContext(
    val transport: ChannelTransport,
    val serialization: Wirespec.Serialization,
) : AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<WirespecChannelContext>
}

internal suspend fun currentChannelContext(): WirespecChannelContext = coroutineContext[WirespecChannelContext] ?: error(
    "No WirespecChannelContext configured. Register " +
        "`WirespecChannelExtension(channel)` on the spec.",
)

/** The minimal publish surface a message broker must expose to back the send side of the channel scenario DSL. */
fun interface ChannelTransport {
    /** Publish a single serialized message [body] to [topic] under an optional [key]. */
    suspend fun publish(topic: String, key: String?, body: ByteArray)
}
