package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.runtime.CallExecutor
import community.flock.wirespec.kotlin.Wirespec
import io.kotest.property.Gen
import kotlin.reflect.KClass

/** Build a [ResponseBuilder] for one response variant of an endpoint. */
fun responseCall(
    endpointObject: Wirespec.Endpoint,
    variantClass: KClass<*>,
): ResponseBuilder = ResponseBuilder(endpointObject, variantClass)

/** Builds a single random `Response<status>` instance for an endpoint. */
@WirespecScenarioDsl
class ResponseBuilder internal constructor(
    internal val endpointObject: Wirespec.Endpoint,
    internal val variantClass: KClass<*>,
) {

    internal var bodyGen: Gen<*>? = null

    internal val headerGens: MutableMap<String, Gen<*>> = mutableMapOf()

    /** Pin the whole response body. Called by the generated `body = …` setter. */
    fun body(gen: Gen<*>): ResponseBuilder = apply { bodyGen = gen }

    /** Pin a single response header field. Called by the generated header setters. */
    fun headerGen(name: String, gen: Gen<*>): ResponseBuilder = apply { headerGens[name] = gen }

    /** A [Gen] materialising the random response variant on each draw. */
    fun buildGen(): Gen<Any> = CallExecutor.buildResponseGen(this)
}
