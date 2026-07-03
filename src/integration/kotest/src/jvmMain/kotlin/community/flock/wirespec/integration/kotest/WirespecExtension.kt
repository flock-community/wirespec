package community.flock.wirespec.integration.kotest

import community.flock.wirespec.integration.kotest.runtime.WirespecAmbient
import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import io.kotest.property.RandomSource
import kotlinx.coroutines.withContext

// NB: kotest 6.x relocated TestResult to io.kotest.engine.test; io.kotest.core.test.TestResult no longer exists.

/**
 * Installs an ambient wirespec context around every test so wrapper-free endpoint
 * and channel `*.call { … }` calls resolve their transport and a per-test
 * [RandomSource].
 *
 * Register it from the spec body, passing the transport context(s) the spec drives:
 *
 * ```
 * class MySpec : FunSpec({
 *     extension(WirespecExtension(endpoint = myEndpointContext))
 *     // …
 * })
 * ```
 *
 * Supply [endpoint] for endpoint calls, [channel] for channel calls, or both.
 */
class WirespecExtension(
    private val endpoint: WirespecTestContext? = null,
    private val channel: WirespecChannelContext? = null,
) : TestCaseExtension {
    override suspend fun intercept(
        testCase: TestCase,
        execute: suspend (TestCase) -> TestResult,
    ): TestResult {
        val ambient = WirespecAmbient(
            endpoint = endpoint,
            channel = channel,
            randomSource = RandomSource.seeded(System.nanoTime()),
        )
        return withContext(ambient) { execute(testCase) }
    }
}
