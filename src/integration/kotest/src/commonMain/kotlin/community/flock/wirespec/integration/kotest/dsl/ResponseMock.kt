package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.context.MockStub
import community.flock.wirespec.integration.kotest.runtime.currentAmbient
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Gen

/**
 * Draw one response from [responseGen] (seeded by the ambient `RandomSource`), serialize it
 * through the mock context's [Wirespec.Serialization], and register it with the ambient
 * [MockServer][community.flock.wirespec.integration.kotest.context.MockServer]: every incoming request
 * to this endpoint that satisfies [predicate] gets that response. Backs the generated
 * `Gen<Endpoint.Response<*>>.mock { req -> … }` extension — the response-side twin of
 * `Gen<Request>.call()`, so `PutTodo.generate.response200 { … }.mock { it.path.id == "1" }`
 * stubs exactly the response the `Gen` produces.
 *
 * The typed [predicate] is lowered onto the raw request the server receives: each candidate is
 * deserialized back into the endpoint's `Req` via the server edge before the predicate runs, so
 * a predicate that throws (e.g. a request that does not belong to this endpoint slipping through
 * the method/path matcher) simply fails to match rather than propagating.
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
            matches = { raw ->
                try {
                    predicate(edge.from(raw))
                } catch (_: Throwable) {
                    false
                }
            },
            response = edge.to(response),
        ),
    )
}
