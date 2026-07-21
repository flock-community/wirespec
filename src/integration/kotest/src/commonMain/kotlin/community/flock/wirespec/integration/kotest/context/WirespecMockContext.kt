package community.flock.wirespec.integration.kotest.context

import community.flock.wirespec.kotlin.Wirespec

/**
 * Framework-neutral handle the mock half of the scenario DSL consumes. A [MockServer]
 * registers canned HTTP responses so the system-under-test's client can hit a real
 * socket; a [Wirespec.Serialization] turns a drawn typed [Wirespec.Response] into the
 * raw bytes the stub replies with (and turns each incoming request back into the typed
 * request the `.mock { req -> … }` predicate reads).
 *
 * Supplied per-spec to
 * [WirespecMockExtension][community.flock.wirespec.integration.kotest.extension.WirespecMockExtension],
 * mirroring how a [WirespecChannelContext] carries a [ChannelTransport].
 */
class WirespecMockContext(
    val server: MockServer,
    val serialization: Wirespec.Serialization,
)

/**
 * The minimal stub-registration surface a mock HTTP server must expose to back the
 * response side of the scenario DSL. It deals only in [Wirespec.RawRequest] /
 * [Wirespec.RawResponse] so it stays free of the typed request/response machinery — the
 * generated `Gen<Endpoint.Response<*>>.mock { req -> … }` extension adapts its typed
 * predicate into [MockStub.matches] and serializes the drawn response into
 * [MockStub.response] before handing the stub over.
 *
 * Implementations (e.g. a WireMock-backed server) are supplied per-spec to
 * [WirespecMockExtension][community.flock.wirespec.integration.kotest.extension.WirespecMockExtension]
 * as part of a [WirespecMockContext].
 */
interface MockServer {
    /** Register [stub]: incoming requests to its endpoint that satisfy its matcher get its response. */
    fun stub(stub: MockStub)

    /** Drop every registered stub so the next scenario starts from a clean slate. */
    fun reset()
}

/**
 * One canned response registered with a [MockServer]. [method] and [pathTemplate] come
 * from the endpoint's `Wirespec.Server` (so the server can route by HTTP method and path,
 * path parameters matching any non-slash segment); [matches] is the typed `.mock`
 * predicate lowered to operate on the deserialized raw request; [response] is the drawn,
 * already-serialized reply.
 */
class MockStub(
    val method: String,
    val pathTemplate: String,
    val matches: (Wirespec.RawRequest) -> Boolean,
    val response: Wirespec.RawResponse,
)
