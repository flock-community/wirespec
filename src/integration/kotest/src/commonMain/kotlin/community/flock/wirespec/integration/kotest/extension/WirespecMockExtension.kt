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

fun <T : MockServer> WirespecMockExtension(
    serialization: suspend () -> Wirespec.Serialization,
    server: suspend () -> T,
): WirespecMockExtension = WirespecMockExtension(
    serverFactory = { server() },
    serializationFactory = serialization,
    resetBeforeTest = true,
    closeAfterSpec = true,
)

fun WirespecMockExtension(
    server: MockServer,
    serialization: suspend () -> Wirespec.Serialization,
): WirespecMockExtension = WirespecMockExtension(
    serverFactory = { server },
    serializationFactory = serialization,
    resetBeforeTest = true,
    closeAfterSpec = false,
)

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

interface MockServer {
    fun stub(stub: MockStub)

    fun reset()
}

class MockStub(
    val method: String,
    val pathTemplate: String,
    val matches: (Wirespec.RawRequest) -> Boolean,
    val response: Wirespec.RawResponse,
)
