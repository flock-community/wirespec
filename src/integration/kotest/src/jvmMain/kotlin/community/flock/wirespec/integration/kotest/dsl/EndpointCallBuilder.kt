package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.runtime.CallExecutor
import community.flock.wirespec.integration.kotest.runtime.currentRandomSource
import community.flock.wirespec.integration.kotest.validation.EndpointReflection
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.RandomSource

@DslMarker
public annotation class WirespecScenarioDsl

public fun <BodyT : Any, Req : Wirespec.Request<BodyT>, Resp : Wirespec.Response<*>> endpointCall(
    client: Wirespec.Client<Req, Resp>,
    endpointObject: Wirespec.Endpoint,
): EndpointCallBuilder<BodyT, Req, Resp> = EndpointCallBuilder(client, endpointObject)

public suspend fun <BodyT : Any, Req : Wirespec.Request<BodyT>, Resp : Wirespec.Response<*>> requestCall(
    client: Wirespec.Client<Req, Resp>,
    endpointObject: Wirespec.Endpoint,
    requestGen: Gen<Req>,
): Resp {
    val request = requestGen.draw(currentRandomSource())
    @Suppress("UNCHECKED_CAST")
    return CallExecutor.executeRequest(client, endpointObject, request) as Resp
}

@WirespecScenarioDsl
public class EndpointCallBuilder<BodyT : Any, Req : Wirespec.Request<BodyT>, Resp : Wirespec.Response<*>> internal constructor(
    @PublishedApi internal val client: Wirespec.Client<Req, Resp>,
    endpointObject: Wirespec.Endpoint,
) {

    @PublishedApi
    internal val reflection: EndpointReflection = EndpointReflection.of(endpointObject)

    @PublishedApi internal var bodyTransform: ((Any, RandomSource) -> Any)? = null

    @PublishedApi internal var bodyListSizeGen: Gen<Int>? = null

    @PublishedApi internal val pathGens: MutableMap<String, Gen<*>> = mutableMapOf()

    @PublishedApi internal val queryGens: MutableMap<String, Gen<*>> = mutableMapOf()

    @PublishedApi internal val headerGens: MutableMap<String, Gen<*>> = mutableMapOf()

    public fun bodyTransform(transform: (Any, RandomSource) -> Any): EndpointCallBuilder<BodyT, Req, Resp> = apply {
        bodyTransform = transform
    }

    public fun bodyListSize(gen: Gen<Int>): EndpointCallBuilder<BodyT, Req, Resp> = apply { bodyListSizeGen = gen }

    public fun pathGen(name: String, gen: Gen<*>): EndpointCallBuilder<BodyT, Req, Resp> = apply { pathGens[name] = gen }

    public fun queryGen(name: String, gen: Gen<*>): EndpointCallBuilder<BodyT, Req, Resp> = apply { queryGens[name] = gen }

    public fun headerGen(name: String, gen: Gen<*>): EndpointCallBuilder<BodyT, Req, Resp> = apply { headerGens[name] = gen }

    public fun buildRequestGen(): Arb<Req> {
        @Suppress("UNCHECKED_CAST")
        return CallExecutor.buildRequestGen(this) as Arb<Req>
    }
}
