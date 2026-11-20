package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.extension.MockStub
import community.flock.wirespec.integration.kotest.extension.WirespecMockContext
import community.flock.wirespec.integration.kotest.extension.currentMockContext
import community.flock.wirespec.integration.kotest.runtime.currentRandomSource
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Gen

public suspend fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> responseMock(
    server: Wirespec.Server<Req, Res>,
    responseGen: Gen<Res>,
    predicate: (Req) -> Boolean,
): Unit = currentMockContext().let { ctx ->
    ctx.stub(server, server.server(ctx.serialization).to(responseGen.draw(currentRandomSource())), predicate)
}

/** A response [Gen], and what changes in its raw form before it is mocked. */
public class ModifiedResponse<Res : Wirespec.Response<*>>(
    internal val responseGen: Gen<Res>,
    internal val modification: RawResponseScope.() -> Unit,
)

public suspend fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> modifiedResponseMock(
    server: Wirespec.Server<Req, Res>,
    response: ModifiedResponse<Res>,
    predicate: (Req) -> Boolean,
): Unit = currentMockContext().let { ctx ->
    server.server(ctx.serialization).to(response.responseGen.draw(currentRandomSource()))
        .let { raw -> RawResponseScope(raw).apply(response.modification).toWirespec(ctx.serialization) }
        .let { raw -> ctx.stub(server, raw, predicate) }
}

private fun <Req : Wirespec.Request<*>> WirespecMockContext.stub(
    api: Wirespec.Server<Req, *>,
    response: Wirespec.RawResponse,
    predicate: (Req) -> Boolean,
) {
    val edge = api.server(serialization)
    server.stub(
        MockStub(
            method = api.method,
            pathTemplate = api.pathTemplate,
            matches = { raw -> runCatching { predicate(edge.from(raw)) }.getOrDefault(false) },
            response = response,
        ),
    )
}
