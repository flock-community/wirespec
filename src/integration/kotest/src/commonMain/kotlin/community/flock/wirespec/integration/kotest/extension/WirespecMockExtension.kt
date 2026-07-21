package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.integration.kotest.context.MockServer
import community.flock.wirespec.integration.kotest.context.WirespecMockContext
import community.flock.wirespec.integration.kotest.runtime.WirespecAmbient
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import io.kotest.property.RandomSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

// NB: kotest 6.x relocated TestResult to io.kotest.engine.test; io.kotest.core.test.TestResult no longer exists.

/**
 * Installs the mock half of the ambient wirespec context around every test, so a canned
 * response registered with `SomeEndpoint.generate.response200 { … }.mock { req -> … }`
 * reaches the [MockServer] the system-under-test's client points at.
 *
 * `.mock` is the response-side twin of `Gen<Request>.call()`: `call()` draws a request and
 * sends it, `.mock` draws a response and stubs it for every incoming request its predicate
 * accepts.
 *
 * Three registration forms, differing only in who owns the server's lifecycle:
 *
 * ```
 * // eager context/value — you own the server (start + close) and its base URL. Stubs are NOT reset.
 * extension(WirespecMockExtension(myMockServer, mySerialization))
 *
 * // shared server, lazy serialization — you own the server (e.g. one started in a ProjectConfig,
 * // whose base URL feeds the app before the suite starts), the extension resolves serialization per
 * // test (e.g. Spring via `testContextManager()`) and resets stubs before each test but never closes it.
 * extension(WirespecMockExtension(server = myMockServer, serialization = { myWirespecSerialization() }))
 *
 * // managed — built once per spec (lazily) from `suspend` factories, reset before each test, and
 * // closed after the spec.
 * extension(WirespecMockExtension(serialization = { myWirespecSerialization() }, server = { WireMockMockServer.start() }))
 * ```
 *
 * Composes with [WirespecEndpointExtension] and [WirespecChannelExtension]: when several are
 * registered they merge into a single ambient (sharing one per-test [RandomSource]), so a spec
 * can drive live endpoints, channels and mocked dependencies together.
 */
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

    // Server resolved once per spec (not per instance), so a single instance registered in a
    // ProjectConfig serves every spec correctly. Keyed by Spec; the mutex guards the suspend build.
    private val servers = ConcurrentHashMap<Spec, MockServer>()
    private val buildLock = Mutex()

    override suspend fun intercept(
        testCase: TestCase,
        execute: suspend (TestCase) -> TestResult,
    ): TestResult {
        val server = server(testCase.spec)
        if (resetBeforeTest) server.reset()
        val mock = WirespecMockContext(server, serializationFactory())
        val ambient = coroutineContext[WirespecAmbient]?.withMock(mock)
            ?: WirespecAmbient(endpoint = null, channel = null, mock = mock, randomSource = RandomSource.seeded(System.nanoTime()))
        return withContext(ambient) { execute(testCase) }
    }

    private suspend fun server(spec: Spec): MockServer = servers[spec] ?: buildLock.withLock {
        servers[spec] ?: serverFactory(spec).also { servers[spec] = it }
    }

    override suspend fun afterSpec(spec: Spec) {
        val removed = servers.remove(spec)
        // Only a managed server is owned here; an eager/shared one belongs to the caller.
        if (closeAfterSpec) (removed as? AutoCloseable)?.close()
    }
}

/**
 * Managed [WirespecMockExtension]: builds the [MockServer] once per spec (lazily) via a `suspend`
 * factory, resets its stubs before each test, and closes it after the spec. The generic [T] lets a
 * caller keep the concrete server type if they need it elsewhere. Named arguments
 * (`serialization = …`, `server = …`) select this factory over the eager constructors.
 */
fun <T : MockServer> WirespecMockExtension(
    serialization: suspend () -> Wirespec.Serialization,
    server: suspend () -> T,
): WirespecMockExtension = WirespecMockExtension(
    serverFactory = { server() },
    serializationFactory = serialization,
    resetBeforeTest = true,
    closeAfterSpec = true,
)

/**
 * Caller-owned [WirespecMockExtension]: uses a long-lived [server] the caller starts and closes (e.g.
 * one shared across the suite from a ProjectConfig, whose base URL is wired into the app before it
 * boots), resolving [serialization] per test from a `suspend` factory. Stubs are reset before each
 * test; the server is left open (the caller owns its lifecycle). Named arguments select this factory.
 */
fun WirespecMockExtension(
    server: MockServer,
    serialization: suspend () -> Wirespec.Serialization,
): WirespecMockExtension = WirespecMockExtension(
    serverFactory = { server },
    serializationFactory = serialization,
    resetBeforeTest = true,
    closeAfterSpec = false,
)
