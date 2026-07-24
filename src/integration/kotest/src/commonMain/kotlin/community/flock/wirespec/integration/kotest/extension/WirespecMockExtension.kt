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

/** Installs the mock half of the ambient wirespec context around every test. */
class WirespecMockExtension internal constructor(
    private val serverFactory: suspend (Spec) -> MockServer,
    private val serializationFactory: suspend () -> Wirespec.Serialization,
    private val resetBeforeTest: Boolean,
    private val closeAfterSpec: Boolean,
) : TestCaseExtension,
    AfterSpecListener {

    constructor(mock: WirespecMockContext) : this(
        serverFactory = { mock.server },
        serializationFactory = { mock.serialization },
        resetBeforeTest = false,
        closeAfterSpec = false,
    )

    /** Convenience: build the [WirespecMockContext] from a [server] + [serialization] directly. */
    constructor(
        server: MockServer,
        serialization: Wirespec.Serialization,
    ) : this(WirespecMockContext(server, serialization))

    private val servers = SpecScopedResource(closeOnRemove = closeAfterSpec) { spec -> serverFactory(spec) }

    override suspend fun intercept(
        testCase: TestCase,
        execute: suspend (TestCase) -> TestResult,
    ): TestResult {
        val server = servers.get(testCase.spec)
        if (resetBeforeTest) server.reset()
        val mock = WirespecMockContext(server, serializationFactory())
        val seed = coroutineContext[WirespecSeed].orNew()
        return withContext(mock + seed) { execute(testCase) }
    }

    override suspend fun afterSpec(spec: Spec) = servers.remove(spec)
}

/** Managed [WirespecMockExtension] that builds the [MockServer] once per spec from `suspend` factories. */
fun <T : MockServer> WirespecMockExtension(
    serialization: suspend () -> Wirespec.Serialization,
    server: suspend () -> T,
): WirespecMockExtension = WirespecMockExtension(
    serverFactory = { server() },
    serializationFactory = serialization,
    resetBeforeTest = true,
    closeAfterSpec = true,
)

/** Caller-owned [WirespecMockExtension] that uses a long-lived [server] and resolves [serialization] per test. */
fun WirespecMockExtension(
    server: MockServer,
    serialization: suspend () -> Wirespec.Serialization,
): WirespecMockExtension = WirespecMockExtension(
    serverFactory = { server },
    serializationFactory = serialization,
    resetBeforeTest = true,
    closeAfterSpec = false,
)

/** Framework-neutral handle [WirespecMockExtension] installs and the mock half of the scenario DSL consumes. */
class WirespecMockContext(
    val server: MockServer,
    val serialization: Wirespec.Serialization,
) : AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<WirespecMockContext>
}

internal suspend fun currentMockContext(): WirespecMockContext = coroutineContext[WirespecMockContext] ?: error(
    "No WirespecMockContext configured. Register " +
        "`WirespecMockExtension(mock)` on the spec.",
)

/** The minimal stub-registration surface a mock HTTP server must expose to back the response side of the scenario DSL. */
interface MockServer {
    /** Register [stub]: incoming requests to its endpoint that satisfy its matcher get its response. */
    fun stub(stub: MockStub)

    /** Drop every registered stub so the next scenario starts from a clean slate. */
    fun reset()
}

/** One canned response registered with a [MockServer]. */
class MockStub(
    val method: String,
    val pathTemplate: String,
    val matches: (Wirespec.RawRequest) -> Boolean,
    val response: Wirespec.RawResponse,
)
