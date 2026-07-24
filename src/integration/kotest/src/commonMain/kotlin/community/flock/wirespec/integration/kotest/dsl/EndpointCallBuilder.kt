package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.runtime.currentRandomSource
import community.flock.wirespec.integration.kotest.runtime.CallExecutor
import community.flock.wirespec.integration.kotest.validation.EndpointReflection
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Gen
import io.kotest.property.RandomSource

@DslMarker
annotation class WirespecScenarioDsl

/** Build an [EndpointCallBuilder] for an endpoint. */
fun <BodyT : Any, Req : Wirespec.Request<BodyT>, Resp : Wirespec.Response<*>> endpointCall(
    client: Wirespec.Client<Req, Resp>,
    endpointObject: Wirespec.Endpoint,
): EndpointCallBuilder<BodyT, Req, Resp> = EndpointCallBuilder(client, endpointObject)

/** Draw one request from [requestGen], transport it, and validate the response against the contract. */
suspend fun <BodyT : Any, Req : Wirespec.Request<BodyT>, Resp : Wirespec.Response<*>> requestCall(
    client: Wirespec.Client<Req, Resp>,
    endpointObject: Wirespec.Endpoint,
    requestGen: Gen<Req>,
): Resp {
    val request = requestGen.draw(currentRandomSource())
    @Suppress("UNCHECKED_CAST")
    return CallExecutor.executeRequest(client, endpointObject, request) as Resp
}

@WirespecScenarioDsl
class EndpointCallBuilder<BodyT : Any, Req : Wirespec.Request<BodyT>, Resp : Wirespec.Response<*>> internal constructor(
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

    /** Reconstruct the request body from per-field override `Gen`s. */
    fun bodyTransform(transform: (Any, RandomSource) -> Any): EndpointCallBuilder<BodyT, Req, Resp> = apply {
        bodyTransform = transform
    }

    /** Number of elements to generate when the request body is a list. */
    fun bodyListSize(gen: Gen<Int>): EndpointCallBuilder<BodyT, Req, Resp> = apply { bodyListSizeGen = gen }

    /** Register a per-field path generator. Called by generated `path(...)`. */
    fun pathGen(name: String, gen: Gen<*>): EndpointCallBuilder<BodyT, Req, Resp> = apply { pathGens[name] = gen }

    /** Register a per-field query generator. Called by generated `query(...)`. */
    fun queryGen(name: String, gen: Gen<*>): EndpointCallBuilder<BodyT, Req, Resp> = apply { queryGens[name] = gen }

    /** Register a per-field header generator. Called by generated `header(...)`. */
    fun headerGen(name: String, gen: Gen<*>): EndpointCallBuilder<BodyT, Req, Resp> = apply { headerGens[name] = gen }

    /** A [Gen] materialising the typed request on each draw. */
    fun buildRequestGen(): Gen<Req> {
        @Suppress("UNCHECKED_CAST")
        return CallExecutor.buildRequestGen(this) as Gen<Req>
    }
}
