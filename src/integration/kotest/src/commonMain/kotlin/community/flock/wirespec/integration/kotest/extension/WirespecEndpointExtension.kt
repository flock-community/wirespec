package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.integration.kotest.runtime.WirespecSeed
import community.flock.wirespec.integration.kotest.runtime.orNew
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/** Installs the endpoint half of the ambient wirespec context around every test. */
class WirespecEndpointExtension internal constructor(
    private val eager: WirespecEndpointContext?,
    private val serializationFactory: (suspend () -> Wirespec.Serialization)?,
    private val transportationFactory: (suspend () -> Wirespec.Transportation)?,
) : TestCaseExtension {

    constructor(endpoint: WirespecEndpointContext) : this(endpoint, null, null)

    /** Convenience: build the [WirespecEndpointContext] from a [transportation] + [serialization] directly. */
    constructor(
        transportation: Wirespec.Transportation,
        serialization: Wirespec.Serialization,
    ) : this(WirespecEndpointContext(transportation, serialization))

    override suspend fun intercept(
        testCase: TestCase,
        execute: suspend (TestCase) -> TestResult,
    ): TestResult {
        val endpoint = eager ?: WirespecEndpointContext(transportationFactory!!(), serializationFactory!!())
        val seed = coroutineContext[WirespecSeed].orNew()
        return withContext(endpoint + seed) { execute(testCase) }
    }
}

/** Factory-form [WirespecEndpointExtension] that resolves the transportation and serialization per test. */
fun WirespecEndpointExtension(
    serialization: suspend () -> Wirespec.Serialization,
    transportation: suspend () -> Wirespec.Transportation,
): WirespecEndpointExtension = WirespecEndpointExtension(
    eager = null,
    serializationFactory = serialization,
    transportationFactory = transportation,
)

/** Framework-neutral handle [WirespecEndpointExtension] installs and the scenario DSL consumes. */
class WirespecEndpointContext(
    val transportation: Wirespec.Transportation,
    val serialization: Wirespec.Serialization,
) : AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<WirespecEndpointContext>
}

internal suspend fun currentEndpointContext(): WirespecEndpointContext = coroutineContext[WirespecEndpointContext] ?: error(
    "No WirespecEndpointContext configured. Register " +
        "`WirespecEndpointExtension(endpoint)` on the spec.",
)
