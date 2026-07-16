package community.flock.wirespec.integration.kotest

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
 * Supply the [MockServer] eagerly, or let this extension build and own it:
 *
 * ```
 * // eager — you own the server's lifecycle (and its base URL, so the app client can be
 * // pointed at it before the suite starts). Stubs are NOT reset for you.
 * extension(WirespecMockExtension(myMockServer, mySerialization))
 *
 * // managed — built once per spec (lazily) from `suspend` factories, reset before each test,
 * // closed after the spec. The factories carry any framework wiring (e.g. Spring via
 * // `testContextManager()`), so this extension itself stays framework-agnostic.
 * extension(
 *     WirespecMockExtension(
 *         serialization = { myWirespecSerialization() },
 *         server = { WireMockMockServer.start() },
 *     ),
 * )
 * ```
 *
 * Composes with [WirespecEndpointExtension] and [WirespecChannelExtension]: when several are
 * registered they merge into a single ambient (sharing one per-test [RandomSource]), so a spec
 * can drive live endpoints, channels and mocked dependencies together.
 */
class WirespecMockExtension internal constructor(
    private val eager: WirespecMockContext?,
    private val serializationFactory: (suspend () -> Wirespec.Serialization)?,
    private val serverFactory: (suspend () -> MockServer)?,
) : TestCaseExtension,
    AfterSpecListener {

    constructor(mock: WirespecMockContext) : this(mock, null, null)

    /** Convenience: build the [WirespecMockContext] from a [server] + [serialization] directly. */
    constructor(
        server: MockServer,
        serialization: Wirespec.Serialization,
    ) : this(WirespecMockContext(server, serialization))

    // Managed mode builds one server per spec (not per instance), so a single instance registered
    // in a ProjectConfig serves every spec correctly. Keyed by Spec; the mutex guards the suspend build.
    private val servers = ConcurrentHashMap<Spec, MockServer>()
    private val buildLock = Mutex()

    override suspend fun intercept(
        testCase: TestCase,
        execute: suspend (TestCase) -> TestResult,
    ): TestResult {
        val mock = eager ?: run {
            val server = managedServer(testCase.spec)
            server.reset()
            WirespecMockContext(server, serializationFactory!!())
        }
        val ambient = coroutineContext[WirespecAmbient]?.withMock(mock)
            ?: WirespecAmbient(endpoint = null, channel = null, mock = mock, randomSource = RandomSource.seeded(System.nanoTime()))
        return withContext(ambient) { execute(testCase) }
    }

    private suspend fun managedServer(spec: Spec): MockServer = servers[spec] ?: buildLock.withLock {
        servers[spec] ?: serverFactory!!().also { servers[spec] = it }
    }

    override suspend fun afterSpec(spec: Spec) {
        // Only a managed server is owned here; an eager one belongs to the caller.
        (servers.remove(spec) as? AutoCloseable)?.close()
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
    eager = null,
    serializationFactory = serialization,
    serverFactory = server,
)
