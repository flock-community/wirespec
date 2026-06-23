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
 * and channel calls resolve their transport (via the
 * [ContextProvider][community.flock.wirespec.integration.kotest.context.ContextProvider] SPI)
 * and a per-test [RandomSource]. Mount with `@ApplyExtension(WirespecExtension::class)`.
 */
class WirespecExtension : TestCaseExtension {
    override suspend fun intercept(
        testCase: TestCase,
        execute: suspend (TestCase) -> TestResult,
    ): TestResult {
        val ambient = WirespecAmbient(
            spec = testCase.spec,
            randomSource = RandomSource.seeded(System.nanoTime()),
        )
        return withContext(ambient) { execute(testCase) }
    }
}
