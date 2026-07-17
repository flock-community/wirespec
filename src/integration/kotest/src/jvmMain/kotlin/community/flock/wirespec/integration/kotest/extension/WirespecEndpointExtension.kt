package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.integration.kotest.WirespecEndpointContext
import community.flock.wirespec.integration.kotest.runtime.WirespecAmbient
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import io.kotest.property.RandomSource
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

// NB: kotest 6.x relocated TestResult to io.kotest.engine.test; io.kotest.core.test.TestResult no longer exists.

/**
 * Installs the endpoint half of the ambient wirespec context around every test, so
 * wrapper-free `SomeEndpoint.generate.request { … }.call()` calls resolve their
 * transportation and a per-test [RandomSource].
 *
 * Supply the endpoint context eagerly, or from `suspend` factories resolved per test:
 *
 * ```
 * // eager
 * extension(WirespecEndpointExtension(myEndpointContext))
 * extension(WirespecEndpointExtension(myTransportation, mySerialization))
 *
 * // factory form — the factories carry any framework wiring (e.g. the running server's port and a
 * // serialization bean via Spring's `testContextManager()`), so this extension stays
 * // framework-agnostic.
 * extension(
 *     WirespecEndpointExtension(
 *         serialization = { myWirespecSerialization() },
 *         transportation = { HttpTransportation("http://localhost:${'$'}{serverPort()}") },
 *     ),
 * )
 * ```
 *
 * Composes with [WirespecChannelExtension]: when both are registered they merge into a
 * single ambient (sharing one per-test [RandomSource]), so a spec can drive endpoints and
 * channels together.
 */
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
        val ambient = coroutineContext[WirespecAmbient]?.withEndpoint(endpoint)
            ?: WirespecAmbient(endpoint = endpoint, channel = null, mock = null, randomSource = RandomSource.seeded(System.nanoTime()))
        return withContext(ambient) { execute(testCase) }
    }
}

/**
 * Factory-form [WirespecEndpointExtension]: resolves the [transportation] + [serialization] from
 * `suspend` factories per test (an `HttpTransportation` is stateless, so nothing is cached or
 * closed). Named arguments (`serialization = …`, `transportation = …`) select this over the eager
 * constructors.
 */
fun WirespecEndpointExtension(
    serialization: suspend () -> Wirespec.Serialization,
    transportation: suspend () -> Wirespec.Transportation,
): WirespecEndpointExtension = WirespecEndpointExtension(
    eager = null,
    serializationFactory = serialization,
    transportationFactory = transportation,
)
