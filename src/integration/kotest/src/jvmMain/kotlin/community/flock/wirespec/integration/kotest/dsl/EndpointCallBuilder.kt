package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.runtime.CallExecutor
import community.flock.wirespec.integration.kotest.validation.EndpointReflection
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Gen
import io.kotest.property.RandomSource
import kotlin.reflect.KClass
import kotlin.time.Duration

@DslMarker
annotation class WirespecScenarioDsl

/**
 * Build an [EndpointCallBuilder] for an endpoint. Generated `*Dsl` wrappers call
 * this from their `generate.request { … }` entry point.
 */
fun <BodyT : Any, Req : Wirespec.Request<BodyT>, Resp : Wirespec.Response<*>> endpointCall(
    client: Wirespec.Client<Req, Resp>,
    endpointObject: Wirespec.Endpoint,
): EndpointCallBuilder<BodyT, Req, Resp> = EndpointCallBuilder(client, endpointObject)

/**
 * Send a pre-built typed request through the ambient transport and validate the response
 * against the contract (any declared status). Backs the generated `<Endpoint>.Request.call()`
 * extension, so a request materialised with `generate.request { … }` can be sent as-is:
 * `PutTodo.generate.request { … }.call()`.
 */
suspend fun <BodyT : Any, Req : Wirespec.Request<BodyT>, Resp : Wirespec.Response<*>> requestCall(
    client: Wirespec.Client<Req, Resp>,
    endpointObject: Wirespec.Endpoint,
    request: Req,
): Resp {
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

    internal var expectedStatuses: Set<Int>? = null

    // Invoked with the generated request and the validated response, so assertion blocks can read
    // both (e.g. assert the response echoes a generated request field). The response-only
    // `expecting { … }` form ignores the request argument.
    internal var customAssertion: ((request: Any, response: Any) -> Unit)? = null

    /**
     * Reconstruct the request body from per-field override `Gen`s. The generated typed
     * body builder passes a transform that takes the contract-derived default body (drawn
     * by the kotest generator) plus the call's [RandomSource] and returns a copy with the
     * overridden fields replaced — `base.copy(field = gen.draw(rs))`. Each un-overridden
     * field keeps its generated default. Public (not internal) because generated `*Dsl`
     * classes live in a downstream module and are not inline. Not intended for direct test use.
     */
    fun bodyTransform(transform: (Any, RandomSource) -> Any): EndpointCallBuilder<BodyT, Req, Resp> = apply {
        bodyTransform = transform
    }

    /**
     * Number of elements to generate when the request body is a list. Called by the
     * generated list-body wrappers (`bodyCount`). Without it the runtime draws 1..3.
     */
    fun bodyListSize(gen: Gen<Int>): EndpointCallBuilder<BodyT, Req, Resp> = apply { bodyListSizeGen = gen }

    /** Register a per-field path generator. Called by generated `path(...)`. */
    fun pathGen(name: String, gen: Gen<*>): EndpointCallBuilder<BodyT, Req, Resp> = apply { pathGens[name] = gen }

    /** Register a per-field query generator. Called by generated `query(...)`. */
    fun queryGen(name: String, gen: Gen<*>): EndpointCallBuilder<BodyT, Req, Resp> = apply { queryGens[name] = gen }

    /** Register a per-field header generator. Called by generated `header(...)`. */
    fun headerGen(name: String, gen: Gen<*>): EndpointCallBuilder<BodyT, Req, Resp> = apply { headerGens[name] = gen }

    /**
     * Build the typed request object from the registered slot/body gens against the ambient
     * `RandomSource`, without sending it. Backs the generated `<Endpoint>.request { … }` DSL;
     * `call { … }` sends exactly this request.
     */
    suspend fun buildRequest(): Req {
        @Suppress("UNCHECKED_CAST")
        return CallExecutor.buildRequest(this) as Req
    }

    // ---- terminals (eager, suspend) ----

    suspend inline fun <reified R : Resp> expecting(): R = expecting(R::class)

    suspend fun <R : Resp> expecting(variantClass: KClass<R>): R {
        expectedStatuses = setOf(statusOf(variantClass))
        @Suppress("UNCHECKED_CAST")
        return CallExecutor.executeEndpoint(this) as R
    }

    suspend inline fun <reified R : Resp> expecting(noinline block: (R) -> Unit): R = expecting(R::class, block)

    suspend fun <R : Resp> expecting(variantClass: KClass<R>, block: (R) -> Unit): R {
        expectedStatuses = setOf(statusOf(variantClass))
        @Suppress("UNCHECKED_CAST")
        customAssertion = { _, response -> block(response as R) }
        @Suppress("UNCHECKED_CAST")
        return CallExecutor.executeEndpoint(this) as R
    }

    suspend inline fun <reified R : Resp> expecting(noinline block: (Req, R) -> Unit): R = expecting(R::class, block)

    suspend fun <R : Resp> expecting(variantClass: KClass<R>, block: (Req, R) -> Unit): R {
        expectedStatuses = setOf(statusOf(variantClass))
        @Suppress("UNCHECKED_CAST")
        customAssertion = { request, response -> block(request as Req, response as R) }
        @Suppress("UNCHECKED_CAST")
        return CallExecutor.executeEndpoint(this) as R
    }

    // Endpoint streaming is not implemented: the single validated response is
    // delivered as a one-element list. The count/duration arguments are accepted
    // for DSL symmetry with the generated wrappers but not honored.
    suspend inline fun <reified R : Resp> collecting(count: Int, noinline block: (List<R>) -> Unit) = collecting(R::class, block)

    suspend inline fun <reified R : Resp> collecting(duration: Duration, noinline block: (List<R>) -> Unit) = collecting(R::class, block)

    suspend fun <R : Resp> collecting(variantClass: KClass<R>, block: (List<R>) -> Unit) {
        expectedStatuses = setOf(statusOf(variantClass))
        val resp = CallExecutor.executeEndpoint(this)
        @Suppress("UNCHECKED_CAST")
        block(listOf(resp as R))
    }

    @PublishedApi
    internal fun statusOf(variantClass: KClass<*>): Int {
        val name = variantClass.simpleName
            ?: error("Anonymous response variant class — pass a named ResponseNNN class.")
        val match = STATUS_REGEX.matchEntire(name)
            ?: error("Response variant class name '$name' doesn't match ResponseNNN. Use a Wirespec-generated response variant.")
        return match.groupValues[1].toInt()
    }

    companion object {
        @PublishedApi
        internal val STATUS_REGEX = Regex("Response(\\d{3})")
    }
}
