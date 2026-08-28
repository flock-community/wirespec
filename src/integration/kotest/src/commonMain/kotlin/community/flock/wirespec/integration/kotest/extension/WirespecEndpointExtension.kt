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

public class WirespecEndpointExtension internal constructor(
    private val eager: WirespecEndpointContext?,
    private val serializationFactory: (suspend () -> Wirespec.Serialization)?,
    private val transportationFactory: (suspend () -> Wirespec.Transportation)?,
) : TestCaseExtension {

    public constructor(endpoint: WirespecEndpointContext) : this(endpoint, null, null)

    public constructor(
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

public fun WirespecEndpointExtension(
    serialization: suspend () -> Wirespec.Serialization,
    transportation: suspend () -> Wirespec.Transportation,
): WirespecEndpointExtension = WirespecEndpointExtension(
    eager = null,
    serializationFactory = serialization,
    transportationFactory = transportation,
)

public class WirespecEndpointContext(
    public val transportation: Wirespec.Transportation,
    public val serialization: Wirespec.Serialization,
) : AbstractCoroutineContextElement(Key) {

    public companion object Key : CoroutineContext.Key<WirespecEndpointContext>
}

internal suspend fun currentEndpointContext(): WirespecEndpointContext = coroutineContext[WirespecEndpointContext] ?: error(
    "No WirespecEndpointContext configured. Register " +
        "`WirespecEndpointExtension(endpoint)` on the spec.",
)
