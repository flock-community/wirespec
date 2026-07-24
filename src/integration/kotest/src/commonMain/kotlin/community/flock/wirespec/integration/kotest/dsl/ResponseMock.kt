package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.runtime.currentRandomSource
import community.flock.wirespec.integration.kotest.extension.MockStub
import community.flock.wirespec.integration.kotest.extension.currentMockContext
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Gen

/** Draw one response from [responseGen], serialize it, and stub it on the ambient mock server for requests matching [predicate]. */
suspend fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> responseMock(
    server: Wirespec.Server<Req, Res>,
    responseGen: Gen<Res>,
    predicate: (Req) -> Boolean,
) {
    val ctx = currentMockContext()
    val edge = server.server(ctx.serialization)
    val response = responseGen.draw(currentRandomSource())
    ctx.server.stub(
        MockStub(
            method = server.method,
            pathTemplate = server.pathTemplate,
            matches = { raw -> runCatching { predicate(edge.from(raw)) }.getOrDefault(false) },
            response = edge.to(response),
        ),
    )
}
