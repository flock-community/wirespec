package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.context.MockStub
import community.flock.wirespec.integration.kotest.runtime.currentAmbient
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Gen

/**
 * Draw one response from [responseGen], serialize it, and stub it on the ambient mock server:
 * every incoming request to this endpoint satisfying [predicate] gets that response. Backs the
 * generated `Gen<Endpoint.Response<*>>.mock { req -> … }`.
 *
 * The typed [predicate] runs against the raw request deserialized back into `Req` via the server
 * edge; a predicate that throws (e.g. a foreign request slipping through the method/path matcher)
 * fails to match rather than propagating.
 */
suspend fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> responseMock(
    server: Wirespec.Server<Req, Res>,
    responseGen: Gen<Res>,
    predicate: (Req) -> Boolean,
) {
    val ambient = currentAmbient()
    val ctx = ambient.mockContext()
    val edge = server.server(ctx.serialization)
    val response = responseGen.draw(ambient.randomSource)
    ctx.server.stub(
        MockStub(
            method = server.method,
            pathTemplate = server.pathTemplate,
            matches = { raw -> runCatching { predicate(edge.from(raw)) }.getOrDefault(false) },
            response = edge.to(response),
        ),
    )
}
